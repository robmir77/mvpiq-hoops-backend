package com.mvpiq.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mvpiq.dto.RealtimeStatsResponse;
import com.mvpiq.service.WorkoutService;
import io.quarkus.scheduler.Scheduled;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/api/workouts/live/{sessionId}")
public class WorkoutLiveSocket {

    private static final Logger LOGGER = Logger.getLogger(WorkoutLiveSocket.class.getName());

    private static final Map<String, Session>  sessions         = new ConcurrentHashMap<>();
    private static final Map<String, UUID>     sessionToUserMap = new ConcurrentHashMap<>();

    @Inject WorkoutService workoutService;
    @Inject ObjectMapper   objectMapper;

    @OnOpen
    public void onOpen(Session session, @PathParam("sessionId") String sessionId) {
        // ✅ Fix: null-check su userId — .get("userId") restituisce null
        // se il parametro manca nell'URL, e .get(0) lancia NullPointerException
        List<String> userIdParams = session.getRequestParameterMap().get("userId");
        if (userIdParams == null || userIdParams.isEmpty()) {
            LOGGER.warn("WebSocket onOpen: missing userId for session " + sessionId + " — closing");
            try { session.close(new CloseReason(
                    CloseReason.CloseCodes.VIOLATED_POLICY, "userId required")); }
            catch (Exception ignored) {}
            return;
        }

        try {
            UUID userId = UUID.fromString(userIdParams.get(0));
            sessions.put(sessionId, session);
            sessionToUserMap.put(sessionId, userId);
            LOGGER.info("WebSocket opened for session: " + sessionId + " user: " + userId);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("WebSocket onOpen: invalid userId for session " + sessionId);
            try { session.close(new CloseReason(
                    CloseReason.CloseCodes.VIOLATED_POLICY, "invalid userId")); }
            catch (Exception ignored) {}
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("sessionId") String sessionId) {
        sessions.remove(sessionId);
        sessionToUserMap.remove(sessionId);
        LOGGER.info("WebSocket closed for session: " + sessionId);
    }

    @OnError
    public void onError(Session session, @PathParam("sessionId") String sessionId, Throwable error) {
        LOGGER.error("WebSocket error for session: " + sessionId, error);
        sessions.remove(sessionId);
        sessionToUserMap.remove(sessionId);
    }

    @OnMessage
    public void onMessage(String message, @PathParam("sessionId") String sessionId) {
        // Il FE può inviare ping o comandi; per ora solo logging
        LOGGER.debug("WebSocket message for session: " + sessionId + " — " + message);
    }

    // Broadcast stats ogni secondo a tutti i client connessi
    @Scheduled(every = "1s")
    public void broadcastStats() {
        for (Map.Entry<String, Session> entry : sessions.entrySet()) {
            String  sessionId  = entry.getKey();
            Session wsSession  = entry.getValue();

            if (!wsSession.isOpen()) continue;

            try {
                UUID userId = sessionToUserMap.get(sessionId);
                if (userId == null) continue;

                RealtimeStatsResponse stats = workoutService.getRealtimeStats(
                        UUID.fromString(sessionId), userId);

                String json = objectMapper.writeValueAsString(stats);
                wsSession.getAsyncRemote().sendText(json);

            } catch (Exception e) {
                LOGGER.error("Error broadcasting stats for session: " + sessionId, e);
            }
        }
    }

    // API pubblica per push manuale (usata da WorkoutService dopo ogni shot)
    public void pushStats(String sessionId, RealtimeStatsResponse stats) {
        Session wsSession = sessions.get(sessionId);
        if (wsSession == null || !wsSession.isOpen()) return;
        try {
            String json = objectMapper.writeValueAsString(stats);
            wsSession.getAsyncRemote().sendText(json);
        } catch (Exception e) {
            LOGGER.error("Error pushing stats for session: " + sessionId, e);
        }
    }
}
