package com.mvpiq.dto.notification;

import com.mvpiq.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    
    private UUID id;
    private UUID userId;
    private String title;
    private String message;
    private NotificationType type;
    private Object data;
    private OffsetDateTime sentAt;
    private OffsetDateTime readAt;
    private OffsetDateTime createdAt;
    private boolean read;

    public static NotificationDTO fromEntity(com.mvpiq.model.Notification notification) {
        if (notification == null) {
            return null;
        }

        return NotificationDTO.builder()
                .id(notification.getId())
                .userId(notification.getUser() != null ? notification.getUser().getId() : null)
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .data(notification.getData())
                .sentAt(notification.getSentAt())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .read(notification.isRead())
                .build();
    }
}
