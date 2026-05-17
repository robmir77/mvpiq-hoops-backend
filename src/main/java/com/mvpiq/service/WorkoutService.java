package com.mvpiq.service;

import com.mvpiq.dto.ShotEventRequest;
import com.mvpiq.dto.WorkoutSessionRequest;
import com.mvpiq.dto.WorkoutSessionResponse;
import com.mvpiq.exception.ResourceNotFoundException;
import com.mvpiq.model.CourtCalibration;
import com.mvpiq.model.Player;
import com.mvpiq.model.ShotEvent;
import com.mvpiq.model.WorkoutSession;
import com.mvpiq.repositories.CourtCalibrationRepository;
import com.mvpiq.repositories.PlayerRepository;
import com.mvpiq.repositories.ShotEventRepository;
import com.mvpiq.repositories.WorkoutSessionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.jboss.logging.Logger;

@ApplicationScoped
public class WorkoutService {

    private static final Logger LOGGER = Logger.getLogger(WorkoutService.class.getName());

    @Inject
    WorkoutSessionRepository workoutSessionRepository;

    @Inject
    ShotEventRepository shotEventRepository;

    @Inject
    CourtCalibrationRepository courtCalibrationRepository;

    @Inject
    PlayerRepository playerRepository;

    @Transactional
    public WorkoutSession createWorkoutSession(UUID playerId, WorkoutSessionRequest request) {
        Player player = playerRepository.findByIdOptional(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found with id: " + playerId));

        // Check if there's an active session
        workoutSessionRepository.findActiveSessionByPlayer(playerId)
                .ifPresent(activeSession -> {
                    throw new IllegalStateException("Player already has an active workout session");
                });

        WorkoutSession session = WorkoutSession.builder()
                .player(player)
                .cameraMode(request.getCameraMode())
                .courtType(request.getCourtType())
                .startTime(OffsetDateTime.now())
                .totalShots(0)
                .madeShots(0)
                .sessionStatus("ACTIVE")
                .calibrationData(request.getCalibrationData())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        workoutSessionRepository.persist(session);
        LOGGER.info("Created new workout session: " + session.getId() + " for player: " + playerId);

        return session;
    }

    @Transactional
    public WorkoutSession endWorkoutSession(UUID sessionId, UUID playerId) {
        WorkoutSession session = workoutSessionRepository.findByIdAndPlayer(sessionId, playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Workout session not found"));

        if (!"ACTIVE".equals(session.getSessionStatus())) {
            throw new IllegalStateException("Session is not active");
        }

        session.setEndTime(OffsetDateTime.now());
        session.setSessionStatus("COMPLETED");
        workoutSessionRepository.persist(session);

        LOGGER.info("Ended workout session: " + sessionId);
        return session;
    }

    @Transactional
    public ShotEvent addShotEvent(UUID sessionId, UUID playerId, ShotEventRequest request) {
        WorkoutSession session = workoutSessionRepository.findByIdAndPlayer(sessionId, playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Workout session not found"));

        if (!"ACTIVE".equals(session.getSessionStatus())) {
            throw new IllegalStateException("Cannot add shots to inactive session");
        }

        ShotEvent shot = ShotEvent.builder()
                .workoutSession(session)
                .timestampMs(request.getTimestampMs())
                .shotResult(request.getShotResult())
                .courtX(request.getCourtX())
                .courtY(request.getCourtY())
                .distanceFromHoop(request.getDistanceFromHoop())
                .releaseAngle(request.getReleaseAngle())
                .releaseVelocity(request.getReleaseVelocity())
                .shotArcHeight(request.getShotArcHeight())
                .videoTimestampMs(request.getVideoTimestampMs())
                .detectionConfidence(request.getDetectionConfidence())
                .trackingData(request.getTrackingData())
                .videoClipPath(request.getVideoClipPath())
                .build();

        shotEventRepository.persist(shot);

        // Update session statistics
        updateSessionStatistics(session);

        LOGGER.info("Added shot event: " + shot.getId() + " to session: " + sessionId);
        return shot;
    }

    @Transactional
    public CourtCalibration saveCalibration(UUID sessionId, UUID playerId, CourtCalibration calibration) {
        WorkoutSession session = workoutSessionRepository.findByIdAndPlayer(sessionId, playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Workout session not found"));

        calibration.setWorkoutSession(session);
        courtCalibrationRepository.persist(calibration);

        LOGGER.info("Saved calibration for session: " + sessionId);
        return calibration;
    }

    private void updateSessionStatistics(WorkoutSession session) {
        long totalShots = shotEventRepository.countByWorkoutSession(session.getId());
        long madeShots = shotEventRepository.countByWorkoutSessionAndResult(session.getId(), ShotEvent.ShotResult.MADE);

        session.setTotalShots((int) totalShots);
        session.setMadeShots((int) madeShots);
        workoutSessionRepository.persist(session);
    }

    public WorkoutSessionResponse getSession(UUID sessionId, UUID playerId) {
        WorkoutSession session = workoutSessionRepository.findByIdAndPlayer(sessionId, playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Workout session not found"));

        return WorkoutSessionResponse.from(session);
    }

    public List<WorkoutSessionResponse> getPlayerSessions(UUID playerId) {
        List<WorkoutSession> sessions = workoutSessionRepository.findByPlayer(playerId);
        return sessions.stream()
                .map(WorkoutSessionResponse::from)
                .toList();
    }

    public WorkoutSession getActiveSession(UUID playerId) {
        return workoutSessionRepository.findActiveSessionByPlayer(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("No active workout session found"));
    }

    public List<ShotEvent> getSessionShots(UUID sessionId, UUID playerId) {
        WorkoutSession session = workoutSessionRepository.findByIdAndPlayer(sessionId, playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Workout session not found"));

        return shotEventRepository.findByWorkoutSession(sessionId);
    }

    @Transactional
    public void pauseSession(UUID sessionId, UUID playerId) {
        WorkoutSession session = workoutSessionRepository.findByIdAndPlayer(sessionId, playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Workout session not found"));

        if (!"ACTIVE".equals(session.getSessionStatus())) {
            throw new IllegalStateException("Session is not active");
        }

        session.setSessionStatus("PAUSED");
        workoutSessionRepository.persist(session);
    }

    @Transactional
    public void resumeSession(UUID sessionId, UUID playerId) {
        WorkoutSession session = workoutSessionRepository.findByIdAndPlayer(sessionId, playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Workout session not found"));

        if (!"PAUSED".equals(session.getSessionStatus())) {
            throw new IllegalStateException("Session is not paused");
        }

        session.setSessionStatus("ACTIVE");
        workoutSessionRepository.persist(session);
    }
}
