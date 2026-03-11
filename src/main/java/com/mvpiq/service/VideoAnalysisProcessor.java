package com.mvpiq.service;

import com.mvpiq.model.VideoAnalysisSession;
import com.mvpiq.repositories.VideoAnalysisSessionRepository;
import com.mvpiq.service.storage.SupabaseStorageService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.io.File;
import java.util.UUID;

@ApplicationScoped
public class VideoAnalysisProcessor {

    @Inject
    VideoAnalysisSessionRepository sessionRepository;

    @Inject
    VideoAnalysisFrameService frameService;

    @Inject
    VideoAnalysisAIService aiService;

    @Inject
    SupabaseStorageService storageService;

    public void processSessionAsync(UUID sessionId) {

        VideoAnalysisSession session = sessionRepository.findById(sessionId);

        session.status = "PROCESSING";

        String videoUrl = session.videoUrl;

        File videoFile = storageService.downloadVideo(videoUrl);

        var frames = frameService.extractFrames(videoFile, session, 2.0);

        // 3️⃣ Persistenza dei frame → dentro transazione breve
        frameService.persistFrames(session, frames);

        aiService.analyzeFrames(session, frames);

        session.status = "COMPLETED";

        storageService.deleteVideo(videoUrl);
    }
}
