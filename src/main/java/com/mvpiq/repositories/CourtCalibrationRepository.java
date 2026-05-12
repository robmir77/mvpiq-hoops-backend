package com.mvpiq.repositories;

import com.mvpiq.model.CourtCalibration;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CourtCalibrationRepository implements PanacheRepositoryBase<CourtCalibration, UUID> {

    public List<CourtCalibration> findByWorkoutSession(UUID sessionId) {
        return find("workoutSession.id = ?1 order by createdAt desc", sessionId).list();
    }

    public Optional<CourtCalibration> findLatestByWorkoutSession(UUID sessionId) {
        return find("workoutSession.id = ?1 order by createdAt desc", sessionId).firstResultOptional();
    }

    public Optional<CourtCalibration> findByIdAndSession(UUID id, UUID sessionId) {
        return find("id = ?1 and workoutSession.id = ?2", id, sessionId).firstResultOptional();
    }

    public List<CourtCalibration> findByPlayer(UUID playerId) {
        return find("""
            select c from CourtCalibration c 
            join c.workoutSession w 
            where w.player.id = ?1 
            order by c.createdAt desc
            """, playerId).list();
    }
}
