package com.mvpiq.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mvpiq.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id = UUID.randomUUID();

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonProperty("user_id")
    private User user;

    @Column(name = "title", length = 200, nullable = false)
    @JsonProperty("title")
    private String title;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    @JsonProperty("message")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    @JsonProperty("type")
    private NotificationType type = NotificationType.GENERAL;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data", columnDefinition = "jsonb")
    @JsonProperty("data")
    private Object data;

    @Column(name = "sent_at")
    @JsonProperty("sent_at")
    private OffsetDateTime sentAt;

    @Column(name = "read_at")
    @JsonProperty("read_at")
    private OffsetDateTime readAt;

    @Column(name = "created_at", nullable = false)
    @JsonProperty("created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at")
    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public void markAsRead() {
        this.readAt = OffsetDateTime.now();
    }

    public boolean isRead() {
        return readAt != null;
    }
}
