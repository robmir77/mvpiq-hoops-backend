package com.mvpiq.service.notification;

import com.mvpiq.enums.NotificationType;
import com.mvpiq.model.Notification;
import com.mvpiq.model.User;
import com.mvpiq.repositories.NotificationRepository;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
@Slf4j
public class NotificationService {

    @Inject
    NotificationRepository notificationRepository;

    @Inject
    FirebaseService firebaseService;

    @Transactional
    public Notification createNotification(UUID userId, String title, String message, NotificationType type, Map<String, String> data) {
        Notification notification = Notification.builder()
                .user(User.builder().id(userId).build())
                .title(title)
                .message(message)
                .type(type)
                .data(data)
                .build();

        notificationRepository.persist(notification);
        log.info("Created notification for user {}: {}", userId, title);

        // Send push notification
        if (firebaseService.isInitialized()) {
            firebaseService.sendNotification(userId, title, message, data);
        }

        return notification;
    }

    @Transactional
    public Notification createNotificationWithoutPush(UUID userId, String title, String message, NotificationType type, Map<String, String> data) {
        Notification notification = Notification.builder()
                .user(User.builder().id(userId).build())
                .title(title)
                .message(message)
                .type(type)
                .data(data)
                .build();

        notificationRepository.persist(notification);
        log.info("Created notification without push for user {}: {}", userId, title);

        return notification;
    }

    @Transactional
    public void sendTrainingProgramGeneratedNotification(UUID userId, String programTitle) {
        Map<String, String> data = Map.of(
                "type", "TRAINING_PROGRAM_GENERATED",
                "programTitle", programTitle,
                "action", "OPEN_TRAINING_PROGRAM"
        );

        createNotification(userId, 
                "Programma di Allenamento Generato", 
                "Il tuo nuovo programma di allenamento \"" + programTitle + "\" è pronto!", 
                NotificationType.TRAINING_PROGRAM_GENERATED, 
                data);
    }

    @Transactional
    public void sendGoalAchievedNotification(UUID userId, String goalTitle) {
        Map<String, String> data = Map.of(
                "type", "GOAL_ACHIEVED",
                "goalTitle", goalTitle,
                "action", "OPEN_GOAL"
        );

        createNotification(userId, 
                "Obiettivo Raggiunto!", 
                "Congratulazioni! Hai raggiunto l'obiettivo: " + goalTitle, 
                NotificationType.GOAL_ACHIEVED, 
                data);
    }

    @Transactional
    public void sendTrainingReminderNotification(UUID userId, String sessionTitle) {
        Map<String, String> data = Map.of(
                "type", "TRAINING_REMINDER",
                "sessionTitle", sessionTitle,
                "action", "OPEN_TRAINING_SESSION"
        );

        createNotification(userId, 
                "Promemoria Allenamento", 
                "Non dimenticare la tua sessione: " + sessionTitle, 
                NotificationType.TRAINING_REMINDER, 
                data);
    }

    @Transactional
    public void sendVideoAnalysisCompletedNotification(UUID userId, String analysisResult) {
        Map<String, String> data = Map.of(
                "type", "VIDEO_ANALYSIS_COMPLETED",
                "analysisResult", analysisResult,
                "action", "OPEN_ANALYSIS"
        );

        createNotification(userId, 
                "Analisi Video Completata", 
                "La tua analisi video è pronta: " + analysisResult, 
                NotificationType.VIDEO_ANALYSIS_COMPLETED, 
                data);
    }

    @Transactional
    public void sendTeamInvitationNotification(UUID userId, String teamName) {
        Map<String, String> data = Map.of(
                "type", "TEAM_INVITATION",
                "teamName", teamName,
                "action", "OPEN_TEAM_INVITATION"
        );

        createNotification(userId, 
                "Invito Squadra", 
                "Sei stato invitato a unirti alla squadra: " + teamName, 
                NotificationType.TEAM_INVITATION, 
                data);
    }

    @Transactional
    public void sendSystemAnnouncementNotification(String title, String message) {
        // This would be sent to all users - implementation depends on requirements
        log.info("System announcement: {} - {}", title, message);
    }

    @Transactional
    public void markNotificationAsRead(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId);
        if (notification != null && !notification.isRead()) {
            notification.markAsRead();
            notificationRepository.persist(notification);
            log.info("Marked notification as read: {}", notificationId);
        }
    }

    @Transactional
    public void markAllNotificationsAsRead(UUID userId) {
        notificationRepository.markAsReadByUserId(userId);
        log.info("Marked all notifications as read for user: {}", userId);
    }

    public java.util.List<Notification> getUserNotifications(UUID userId) {
        return notificationRepository.findByUserId(userId);
    }

    public java.util.List<Notification> getUnreadNotifications(UUID userId) {
        return notificationRepository.findUnreadByUserId(userId);
    }

    public java.util.List<Notification> getRecentNotifications(UUID userId, int limit) {
        return notificationRepository.findRecentByUserId(userId, limit);
    }

    public long getUnreadCount(UUID userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    @Transactional
    public void deleteOldNotifications() {
        OffsetDateTime cutoffDate = OffsetDateTime.now().minusMonths(3);
        notificationRepository.deleteOldNotifications(cutoffDate);
        log.info("Deleted notifications older than 3 months");
    }

    @Transactional
    public void deleteUserNotifications(UUID userId) {
        notificationRepository.deleteByUserId(userId);
        log.info("Deleted all notifications for user: {}", userId);
    }
}
