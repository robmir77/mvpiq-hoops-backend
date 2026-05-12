package com.mvpiq.repositories;

import com.mvpiq.model.ShotEvent;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ShotEventRepository implements PanacheRepositoryBase<ShotEvent, UUID> {

    public List<ShotEvent> findByWorkoutSession(UUID sessionId) {
        return find("workoutSession.id = ?1 order by timestampMs", sessionId).list();
    }

    public List<ShotEvent> findByWorkoutSessionAndResult(UUID sessionId, ShotEvent.ShotResult result) {
        return find("workoutSession.id = ?1 and shotResult = ?2 order by timestampMs", sessionId, result).list();
    }

    public Optional<ShotEvent> findByIdAndSession(UUID id, UUID sessionId) {
        return find("id = ?1 and workoutSession.id = ?2", id, sessionId).firstResultOptional();
    }

    public long countByWorkoutSession(UUID sessionId) {
        return count("workoutSession.id = ?1", sessionId);
    }

    public long countByWorkoutSessionAndResult(UUID sessionId, ShotEvent.ShotResult result) {
        return count("workoutSession.id = ?1 and shotResult = ?2", sessionId, result);
    }

    public List<ShotEvent> findByWorkoutSessionWithCoordinates(UUID sessionId) {
        return find("workoutSession.id = ?1 and courtX is not null and courtY is not null order by timestampMs", 
                   sessionId).list();
    }

    public List<ShotEvent> findRecentShotsByPlayer(UUID playerId, int limit) {
        return find("""
            select s from ShotEvent s 
            join s.workoutSession w 
            where w.player.id = ?1 
            order by s.timestampMs desc
            """, playerId).page(0, limit).list();
    }

    public Double calculateAverageDistance(UUID sessionId) {
        return getEntityManager()
                .createQuery("select AVG(s.distanceFromHoop) from ShotEvent s where s.workoutSession.id = ?1 and s.distanceFromHoop is not null", 
                           Double.class)
                .setParameter(1, sessionId)
                .getSingleResult();
    }

    public List<Object[]> getShotDistributionByZone(UUID sessionId) {
        return getEntityManager()
                .createQuery("""
                    select 
                        case 
                            when s.distanceFromHoop <= 4.0 then 'PAINT'
                            when s.distanceFromHoop <= 7.0 then 'MID_RANGE'
                            when s.distanceFromHoop <= 8.0 then 'CORNER'
                            else 'THREE_POINT'
                        end as zone,
                        count(*) as attempts,
                        sum(case when s.shotResult = 'MADE' then 1 else 0 end) as made
                    from ShotEvent s 
                    where s.workoutSession.id = ?1 and s.distanceFromHoop is not null
                    group by zone
                    order by zone
                    """, Object[].class)
                .setParameter(1, sessionId)
                .getResultList();
    }
}
