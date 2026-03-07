package com.mvpiq.repositories;

import com.mvpiq.model.VideoAnalysisSession;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class VideoAnalysisSessionRepository implements PanacheRepository<VideoAnalysisSession> {
    public VideoAnalysisSession findById(UUID sessionId) {
        return VideoAnalysisSession.findById(sessionId);
    }

    public Optional<VideoAnalysisSession> findByIdOptional(UUID sessionId) {
        return Optional.ofNullable(VideoAnalysisSession.findById(sessionId));
    }
}
