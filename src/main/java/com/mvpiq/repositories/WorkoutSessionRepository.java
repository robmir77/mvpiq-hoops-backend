package com.mvpiq.repositories;

import com.mvpiq.model.WorkoutSession;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class WorkoutSessionRepository implements PanacheRepositoryBase<WorkoutSession, UUID> {

    public List<WorkoutSession> findByPlayer(UUID playerId) {
        return find("player.id = ?1 order by startTime desc", playerId).list();
    }

    public Optional<WorkoutSession> findByIdAndPlayer(UUID id, UUID playerId) {
        return find("id = ?1 and player.id = ?2", id, playerId).firstResultOptional();
    }

    public List<WorkoutSession> findByPlayerAndStatus(UUID playerId, String status) {
        return find("player.id = ?1 and sessionStatus = ?2 order by startTime desc", playerId, status).list();
    }

    public List<WorkoutSession> findByPlayerAndDateRange(UUID playerId, OffsetDateTime startDate, OffsetDateTime endDate) {
        return find("player.id = ?1 and startTime between ?2 and ?3 order by startTime desc", 
                   playerId, startDate, endDate).list();
    }

    public Optional<WorkoutSession> findActiveSessionByPlayer(UUID playerId) {
        return find("player.id = ?1 and sessionStatus = 'ACTIVE' order by startTime desc", playerId)
                .firstResultOptional();
    }

    public List<WorkoutSession> findCompletedSessionsByPlayer(UUID playerId) {
        return find("player.id = ?1 and sessionStatus = 'COMPLETED' order by startTime desc", playerId).list();
    }

    public long countByPlayer(UUID playerId) {
        return count("player.id = ?1", playerId);
    }

    public long countCompletedSessionsByPlayer(UUID playerId) {
        return count("player.id = ?1 and sessionStatus = 'COMPLETED'", playerId);
    }
}
