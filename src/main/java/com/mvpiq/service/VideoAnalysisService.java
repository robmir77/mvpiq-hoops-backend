package com.mvpiq.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mvpiq.dto.*;
import com.mvpiq.model.VideoAnalysisSession;
import com.mvpiq.model.VideoAnalysisResult;
import com.mvpiq.model.VideoAnalysisType;
import com.mvpiq.repositories.VideoAnalysisSessionRepository;
import com.mvpiq.repositories.VideoAnalysisTypeRepository;
import com.mvpiq.repositories.VideoAnalysisResultRepository;
import com.mvpiq.service.storage.SupabaseStorageService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.io.File;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class VideoAnalysisService {

    @Inject
    VideoAnalysisTypeRepository typeRepository;

    @Inject
    VideoAnalysisSessionRepository sessionRepository;

    @Inject
    VideoAnalysisResultRepository resultRepository;

    @Inject
    SupabaseStorageService storageService;

    @Inject
    JsonWebToken jwt;

    @Inject
    VideoAnalysisProcessor processor;

    public AnalysisSessionResponseDTO createSession(CreateAnalysisSessionRequestDTO request) {

        var type = typeRepository.findByCode(request.getAnalysisCode())
                .orElseThrow(() -> new RuntimeException("Analysis type not found"));

        VideoAnalysisSession session = newVideoAnalysisSession(request, type);

        // avvia analisi async
        processor.processSessionAsync(session.id);

        AnalysisSessionResponseDTO response = new AnalysisSessionResponseDTO();
        response.id = session.id;
        response.status = session.status;
        response.analysisCode = type.code;
        response.createdAt = session.createdAt;

        return response;
    }

    @Transactional
    VideoAnalysisSession newVideoAnalysisSession(CreateAnalysisSessionRequestDTO request, VideoAnalysisType type) {
        VideoAnalysisSession session = new VideoAnalysisSession();

        session.userId = UUID.fromString(jwt.getSubject());
        session.analysisType = type;
        session.videoUrl = request.getVideoUrl();
        session.videoSeconds = request.getVideoSeconds();
        session.videoSizeMb = request.getVideoSizeMb();
        session.status = "UPLOADED";
        session.createdAt = OffsetDateTime.now();

        sessionRepository.persist(session);
        return session;
    }

    @Transactional
    public AnalysisResultDTO analyzeVideo(VideoAnalysisRequestDTO request) {

        UUID sessionId = request.sessionId;

        VideoAnalysisSession session = sessionRepository.findByIdOptional(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        session.status = "PROCESSING";

        String videoUrl = session.videoUrl;
        File videoFile = null;

        try {

            // 1 download video
            videoFile = storageService.downloadVideo(videoUrl);

            // 2 AI analysis (placeholder)
            int score = 82;

            String detectedErrors = """
                [{"code":"ELBOW_ALIGNMENT","severity":"medium"}]
            """;

            String suggestions = """
                [{"text":"Keep elbow aligned with the basket"}]
            """;

            String aiResponse = """
                {"comment":"Good shooting form but elbow slightly open"}
            """;

            // 3 salva risultato
            VideoAnalysisResult result = new VideoAnalysisResult();
            result.session = session;
            result.score = score;
            result.detectedErrors = detectedErrors;
            result.suggestions = suggestions;
            result.aiResponse = aiResponse;
            result.createdAt = Instant.now();

            resultRepository.persist(result);

            // 4 aggiorna sessione
            session.status = "COMPLETED";
            session.processedAt = Instant.now();

            // 5 risposta API
            AnalysisResultDTO dto = new AnalysisResultDTO();
            dto.sessionId = sessionId;
            dto.score = score;

            ObjectMapper mapper = new ObjectMapper();
            dto.setDetectedErrors(mapper.readValue(result.detectedErrors, new TypeReference<List<String>>() {}));
            dto.setSuggestions(mapper.readValue(result.suggestions, new TypeReference<List<String>>() {}));
            return dto;

        } catch (Exception e) {

            session.status = "FAILED";
            throw new RuntimeException("Video analysis failed", e);

        } finally {

            // cancella video storage
            try {
                storageService.deleteVideo(videoUrl);
            } catch (Exception ignored) {}

            // cancella file temporaneo
            if (videoFile != null && videoFile.exists()) {
                videoFile.delete();
            }
        }
    }

    @Transactional
    public AnalysisResultDTO getResult(UUID sessionId) {

        // 1️⃣ Recupera sessione
        VideoAnalysisSession session = sessionRepository.findByIdOptional(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        // 2️⃣ Recupera risultato associato
        VideoAnalysisResult result = resultRepository.find("session", session)
                .firstResultOptional()
                .orElseThrow(() -> new RuntimeException("Result not found"));

        // 3️⃣ Costruisci DTO
        AnalysisResultDTO dto = new AnalysisResultDTO();
        dto.setSessionId(sessionId);
        dto.setScore(result.score);

        ObjectMapper mapper = new ObjectMapper();
        try {
            dto.setDetectedErrors(
                    result.detectedErrors == null
                            ? List.of()
                            : mapper.readValue(result.detectedErrors, new TypeReference<List<String>>() {})
            );

            dto.setSuggestions(
                    result.suggestions == null
                            ? List.of()
                            : mapper.readValue(result.suggestions, new TypeReference<List<String>>() {})
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse analysis result JSON", e);
        }

        return dto;
    }
}
