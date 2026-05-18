package com.mvpiq.repositories;

import com.mvpiq.model.PoseAnalysis;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PoseAnalysisRepository implements PanacheRepositoryBase<PoseAnalysis, UUID> {

    public List<PoseAnalysis> findByShotEvent(UUID shotEventId) {
        return find("shotEvent.id = ?1", shotEventId).list();
    }

    public List<PoseAnalysis> findBySession(UUID sessionId) {
        return find("shotEvent.workoutSession.id = ?1", sessionId).list();
    }
}
