package com.mvpiq.repositories;

import com.mvpiq.model.AthleteGoal;
import com.mvpiq.model.PlayerProfile;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class AthleteGoalRepository implements PanacheRepositoryBase<AthleteGoal, UUID> {

    public List<AthleteGoal> findByPlayerId(UUID playerId) {
        return list("from AthleteGoal where player.id = ?1", playerId);
    }

    public List<AthleteGoal> findByPlayerIdAndStatus(UUID playerId, String status) {
        PlayerProfile player = new PlayerProfile();
        player.setId(playerId);
        return list("player", player);
    }

    public List<AthleteGoal> findByPlayerIdAndStatusOrderByCreatedAtDesc(UUID playerId, String status) {
        return list("from AthleteGoal where player.id = ?1 and status = ?2 order by createdAt desc", playerId, status);
    }

    public List<AthleteGoal> findByPlayerIdAndCompletedAtBetween(UUID playerId, OffsetDateTime start, OffsetDateTime end) {
        return list("from AthleteGoal where player.id = ?1 and completedAt between ?2 and ?3", playerId, start, end);
    }

    public long countByPlayerIdAndStatus(UUID playerId, String status) {
        return count("from AthleteGoal where player.id = ?1 and status = ?2", playerId, status);
    }

    public List<AthleteGoal> findActiveGoals(UUID playerId) {
        return list("from AthleteGoal where player.id = ?1 and status = 'ACTIVE'", playerId);
    }

    public List<AthleteGoal> findCompletedGoals(UUID playerId) {
        return list("from AthleteGoal where player.id = ?1 and status = 'COMPLETED'", playerId);
    }

    public List<AthleteGoal> findByDueDateBefore(OffsetDateTime date) {
        return list("dueDate < ?1 and status = 'ACTIVE'", date);
    }
}
