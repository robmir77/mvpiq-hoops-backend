package com.mvpiq.service;

import com.mvpiq.dto.AnalysisResultDTO;
import com.mvpiq.dto.AnalysisSessionResponseDTO;
import com.mvpiq.dto.CreateAnalysisSessionRequestDTO;
import com.mvpiq.dto.VideoAnalysisRequestDTO;
import com.mvpiq.model.VideoAnalysisSession;
import com.mvpiq.model.VideoAnalysisResult;
import com.mvpiq.repositories.VideoAnalysisSessionRepository;
import com.mvpiq.repositories.VideoAnalysisTypeRepository;
import com.mvpiq.repositories.VideoAnalysisResultRepository;
import com.mvpiq.service.storage.SupabaseStorageService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.io.File;
import java.time.Instant;
import java.time.OffsetDateTime;
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

    @Transactional
    public AnalysisSessionResponseDTO createSession(CreateAnalysisSessionRequestDTO request) {

        var type = typeRepository.findByCode(request.analysisCode)
                .orElseThrow(() -> new RuntimeException("Analysis type not found"));

        VideoAnalysisSession session = new VideoAnalysisSession();

        session.userId = request.userId;
        session.analysisType = type;
        session.videoUrl = request.videoUrl;
        session.videoSeconds = request.videoSeconds;
        session.videoSizeMb = request.videoSizeMb;
        session.status = "UPLOADED";
        session.createdAt = OffsetDateTime.now();

        sessionRepository.persist(session);

        AnalysisSessionResponseDTO response = new AnalysisSessionResponseDTO();
        response.id = session.id;
        response.status = session.status;
        response.analysisCode = type.code;
        response.createdAt = session.createdAt;

        return response;
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
            dto.detectedErrors = detectedErrors;
            dto.suggestions = suggestions;

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

    public AnalysisResultDTO getResult(UUID sessionId) {
        return null;
    }
}
