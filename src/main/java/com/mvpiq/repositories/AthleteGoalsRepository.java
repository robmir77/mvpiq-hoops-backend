package com.mvpiq.repositories;

import com.mvpiq.model.AthleteGoal;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class AthleteGoalsRepository implements PanacheRepositoryBase<AthleteGoal, UUID> {

    // Trova tutti i goal di un atleta
    public List<AthleteGoal> findByPlayerId(UUID playerId) {
        return list("from AthleteGoal where player.id = ?1", playerId);
    }

    // Conta i goal di un atleta
    public long countByPlayerId(UUID playerId) {
        return count("from AthleteGoal where player.id = ?1", playerId);
    }

    // Conta i goal di un atleta per stato
    public long countByPlayerIdAndStatus(UUID playerId, String status) {
        return count("from AthleteGoal where player.id = ?1 AND status = ?2", playerId, status);
    }

    // Trova goal per atleta, stato e intervallo di completamento
    public List<AthleteGoal> findByPlayerIdAndStatusAndCompletedAtBetween(UUID playerId, String status, OffsetDateTime startDate, OffsetDateTime endDate) {
        return list("from AthleteGoal where player.id = ?1 AND status = ?2 AND completedAt BETWEEN ?3 AND ?4", playerId, status, startDate, endDate);
    }

    // Trova goal per atleta e intervallo di completamento
    public List<AthleteGoal> findByPlayerIdAndCompletedAtBetween(UUID playerId, OffsetDateTime startDate, OffsetDateTime endDate) {
        return list("from AthleteGoal where player.id = ?1 AND completedAt BETWEEN ?2 AND ?3", playerId, startDate, endDate);
    }

    
    // Persist o update
    @Transactional
    public void save(AthleteGoal goal) {
        persist(goal); // Panache gestisce insert e update
    }

    // Cancellazione di un goal
    @Transactional
    public boolean deleteById(UUID goalId) {
        return delete("id", goalId) > 0;
    }
}
