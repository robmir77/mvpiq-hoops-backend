package com.mvpiq.repositories;

import com.mvpiq.model.TrainingSession;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class TrainingSessionRepository implements PanacheRepositoryBase<TrainingSession, UUID> {

    public List<TrainingSession> findByUser(UUID userId) {
        return list("userId = ?1 ORDER BY sessionDate DESC", userId);
    }

    public long countByUser(UUID userId) {
        return count("userId = ?1", userId);
    }
}