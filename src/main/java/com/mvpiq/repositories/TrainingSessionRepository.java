package com.mvpiq.repositories;

import com.mvpiq.model.TrainingSession;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class TrainingSessionRepository implements PanacheRepositoryBase<TrainingSession, UUID> {

    public List<TrainingSession> findByUser(UUID userId) {
        return list("player.id = ?1 ORDER BY sessionDate DESC", userId);
    }

    public long countByUser(UUID userId) {
        return count("player.id = ?1", userId);
    }

    public List<TrainingSession> findByUserIdAndSessionDateAfterOrderBySessionDateDesc(UUID userId, OffsetDateTime date) {
        return list("player.id = ?1 AND sessionDate > ?2 ORDER BY sessionDate DESC", userId, date);
    }

    public List<TrainingSession> findByUserIdAndSessionDateBetweenOrderBySessionDateDesc(UUID userId, OffsetDateTime startDate, OffsetDateTime endDate) {
        return list("player.id = ?1 AND sessionDate BETWEEN ?2 AND ?3 ORDER BY sessionDate DESC", userId, startDate, endDate);
    }

    public List<TrainingSession> findByUserId(UUID userId) {
        return list("player.id = ?1", userId);
    }

    public long countByUserId(UUID userId) {
        return count("player.id = ?1", userId);
    }
}