package com.mvpiq.resource.notification;

import com.mvpiq.dto.ApiResponse;
import com.mvpiq.dto.notification.NotificationDTO;
import com.mvpiq.model.Notification;
import com.mvpiq.service.notification.NotificationService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/api/notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Slf4j
public class NotificationResource {

    @Inject
    NotificationService notificationService;

    @GET
    @Path("/user/{userId}")
    public Response getUserNotifications(@PathParam("userId") UUID userId,
                                     @QueryParam("limit") Integer limit,
                                     @QueryParam("unreadOnly") Boolean unreadOnly) {
        try {
            List<Notification> notifications;

            if (unreadOnly != null && unreadOnly) {
                notifications = notificationService.getUnreadNotifications(userId);
            } else if (limit != null && limit > 0) {
                notifications = notificationService.getRecentNotifications(userId, limit);
            } else {
                notifications = notificationService.getUserNotifications(userId);
            }

            List<NotificationDTO> notificationDTOs = notifications.stream()
                    .map(NotificationDTO::fromEntity)
                    .collect(Collectors.toList());

            return Response.ok(ApiResponse.success(notificationDTOs, "Notifications retrieved successfully"))
                    .build();

        } catch (Exception e) {
            log.error("Error retrieving notifications for user: {}", userId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Failed to retrieve notifications: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/user/{userId}/unread-count")
    public Response getUnreadCount(@PathParam("userId") UUID userId) {
        try {
            long unreadCount = notificationService.getUnreadCount(userId);
            
            return Response.ok(ApiResponse.success(unreadCount, "Unread count retrieved successfully"))
                    .build();

        } catch (Exception e) {
            log.error("Error retrieving unread count for user: {}", userId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Failed to retrieve unread count: " + e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/{notificationId}/read")
    public Response markAsRead(@PathParam("notificationId") UUID notificationId) {
        try {
            notificationService.markNotificationAsRead(notificationId);
            
            return Response.ok(ApiResponse.success(null, "Notification marked as read"))
                    .build();

        } catch (Exception e) {
            log.error("Error marking notification as read: {}", notificationId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Failed to mark notification as read: " + e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/user/{userId}/read-all")
    public Response markAllAsRead(@PathParam("userId") UUID userId) {
        try {
            notificationService.markAllNotificationsAsRead(userId);
            
            return Response.ok(ApiResponse.success(null, "All notifications marked as read"))
                    .build();

        } catch (Exception e) {
            log.error("Error marking all notifications as read for user: {}", userId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Failed to mark all notifications as read: " + e.getMessage()))
                    .build();
        }
    }

    @DELETE
    @Path("/user/{userId}")
    public Response deleteAllNotifications(@PathParam("userId") UUID userId) {
        try {
            notificationService.deleteUserNotifications(userId);
            
            return Response.ok(ApiResponse.success(null, "All notifications deleted"))
                    .build();

        } catch (Exception e) {
            log.error("Error deleting notifications for user: {}", userId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Failed to delete notifications: " + e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/test/{userId}")
    public Response sendTestNotification(@PathParam("userId") UUID userId,
                                     @QueryParam("title") String title,
                                     @QueryParam("message") String message) {
        try {
            if (title == null || title.trim().isEmpty()) {
                title = "Test Notification";
            }
            if (message == null || message.trim().isEmpty()) {
                message = "This is a test notification from MVPiQ Hoops";
            }

            // Create test notification
            notificationService.createNotification(userId, title, message, 
                    com.mvpiq.enums.NotificationType.GENERAL, null);
            
            return Response.ok(ApiResponse.success(null, "Test notification sent"))
                    .build();

        } catch (Exception e) {
            log.error("Error sending test notification to user: {}", userId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Failed to send test notification: " + e.getMessage()))
                    .build();
        }
    }
}
