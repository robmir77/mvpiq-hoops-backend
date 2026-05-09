package com.mvpiq.repositories;

import com.mvpiq.model.VideoAnalysisSession;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class VideoAnalysisSessionRepository implements PanacheRepositoryBase<VideoAnalysisSession, UUID> {

    public List<VideoAnalysisSession> findByUserId(UUID userId) {
        return list("userId", userId);
    }

    public long countByUserId(UUID userId) {
        return count("userId", userId);
    }
}
