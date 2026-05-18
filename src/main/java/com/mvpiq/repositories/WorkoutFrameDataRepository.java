package com.mvpiq.repositories;

import com.mvpiq.model.WorkoutFrameData;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class WorkoutFrameDataRepository implements PanacheRepositoryBase<WorkoutFrameData, UUID> {

    public List<WorkoutFrameData> findBySession(UUID sessionId) {
        return find("session.id = ?1 order by frameTimestamp", sessionId).list();
    }

    public List<WorkoutFrameData> findBySessionWithShots(UUID sessionId) {
        return find("session.id = ?1 and shotDetected = true order by frameTimestamp", sessionId).list();
    }

    public List<WorkoutFrameData> findRecentFrames(UUID sessionId, int limit) {
        return find("session.id = ?1 order by frameTimestamp desc", sessionId)
                .page(0, limit)
                .list();
    }

    public void deleteBySession(UUID sessionId) {
        delete("session.id = ?1", sessionId);
    }
}
