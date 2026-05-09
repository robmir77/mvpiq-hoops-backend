package com.mvpiq.repositories;

import com.mvpiq.model.UserActivityLog;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class UserActivityLogRepository implements PanacheRepositoryBase<UserActivityLog, UUID> {

    public List<UserActivityLog> findByUserId(UUID userId) {
        return list("userId", userId);
    }

    public List<UserActivityLog> findByUserIdOrderByCreatedAtDesc(UUID userId) {
        return list("userId = ?1 order by createdAt desc", userId);
    }

    public List<UserActivityLog> findByUserIdAndActivityType(UUID userId, String activityType) {
        return list("userId = ?1 and activityType = ?2", userId, activityType);
    }

    public List<UserActivityLog> findByUserIdAndActivityTypeAndCreatedAtBetweenOrderByCreatedAtAsc(
            UUID userId, String activityType, OffsetDateTime start, OffsetDateTime end) {
        return list("userId = ?1 and activityType = ?2 and createdAt between ?3 and ?4 order by createdAt asc",
                userId, activityType, start, end);
    }

    public long countByUserIdAndActivityType(UUID userId, String activityType) {
        return count("userId = ?1 and activityType = ?2", userId, activityType);
    }

    public List<UserActivityLog> findByActivityType(String activityType) {
        return list("activityType", activityType);
    }

    public List<UserActivityLog> findByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end) {
        return list("createdAt between ?1 and ?2", start, end);
    }
}
