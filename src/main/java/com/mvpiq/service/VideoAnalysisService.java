package com.mvpiq.service;

import com.mvpiq.dto.AnalysisSessionResponseDTO;
import com.mvpiq.dto.CreateAnalysisSessionRequestDTO;
import com.mvpiq.model.VideoAnalysisSession;
import com.mvpiq.repositories.VideoAnalysisSessionRepository;
import com.mvpiq.repositories.VideoAnalysisTypeRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;

@ApplicationScoped
public class VideoAnalysisService {

    @Inject
    VideoAnalysisTypeRepository typeRepository;

    @Inject
    VideoAnalysisSessionRepository sessionRepository;

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
        session.createdAt = Instant.now();

        sessionRepository.persist(session);

        AnalysisSessionResponseDTO response = new AnalysisSessionResponseDTO();
        response.id = session.id;
        response.status = session.status;
        response.analysisCode = type.code;
        response.createdAt = session.createdAt;

        return response;
    }
}
