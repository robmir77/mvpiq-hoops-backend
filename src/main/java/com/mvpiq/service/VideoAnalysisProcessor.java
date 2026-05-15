package com.mvpiq.service;

import com.mvpiq.model.VideoAnalysisSession;
import com.mvpiq.repositories.VideoAnalysisSessionRepository;
import com.mvpiq.service.storage.SupabaseStorageService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.File;
import java.util.UUID;

@ApplicationScoped
public class VideoAnalysisProcessor {

    @ConfigProperty(name = "mvpiq.hoop.search-frames")
    int searchFrames;

    @Inject
    VideoAnalysisSessionRepository sessionRepository;

    @Inject
    VideoAnalysisFrameService frameService;

    @Inject
    VideoAnalysisAIService aiService;

    @Inject
    SupabaseStorageService storageService;

    @Transactional
    public void processSessionAsync(UUID sessionId) {

        VideoAnalysisSession session = sessionRepository.findById(sessionId);
        if (session == null) {
            throw new RuntimeException("Session not found: " + sessionId);
        }

        try {
            session.status = "PROCESSING";

            String videoUrl = session.videoUrl;
            File videoFile = storageService.downloadVideo(videoUrl);

            var frames = frameService.extractFrames(videoFile, session, searchFrames);

            frameService.persistFrames(session, frames);

            aiService.analyzeFrames(session, frames);

            session.status = "COMPLETED";

            storageService.deleteVideo(videoUrl);
        } catch (Exception e) {
            session.status = "FAILED";
            session.errorMessage = e.getMessage();
            session.retryCount = (session.retryCount == null ? 0 : session.retryCount) + 1;
            throw new RuntimeException("Processing failed for session: " + sessionId, e);
        }
    }
}
