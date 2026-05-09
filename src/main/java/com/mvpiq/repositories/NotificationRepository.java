package com.mvpiq.repositories;

import com.mvpiq.model.Notification;
import com.mvpiq.enums.NotificationType;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class NotificationRepository implements PanacheRepositoryBase<Notification, UUID> {

    public List<Notification> findByUserId(UUID userId) {
        return list("user.id = ?1 ORDER BY createdAt DESC", userId);
    }

    public List<Notification> findUnreadByUserId(UUID userId) {
        return list("user.id = ?1 AND readAt IS NULL ORDER BY createdAt DESC", userId);
    }

    public List<Notification> findByUserIdAndType(UUID userId, NotificationType type) {
        return list("user.id = ?1 AND type = ?2 ORDER BY createdAt DESC", userId, type);
    }

    public List<Notification> findRecentByUserId(UUID userId, int limit) {
        return find("user.id = ?1 ORDER BY createdAt DESC", userId)
                .page(0, limit)
                .list();
    }

    public long countUnreadByUserId(UUID userId) {
        return count("user.id = ?1 AND readAt IS NULL", userId);
    }

    public void markAsReadByUserId(UUID userId) {
        update("readAt = ?1 WHERE user.id = ?2 AND readAt IS NULL", 
                OffsetDateTime.now(), userId);
    }

    public void deleteOldNotifications(OffsetDateTime cutoffDate) {
        delete("createdAt < ?1", cutoffDate);
    }

    public void deleteByUserId(UUID userId) {
        delete("user.id = ?1", userId);
    }
}
