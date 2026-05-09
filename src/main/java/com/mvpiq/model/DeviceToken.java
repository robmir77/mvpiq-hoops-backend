package com.mvpiq.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mvpiq.enums.DevicePlatform;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
@Table(name = "device_tokens")
public class DeviceToken {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id = UUID.randomUUID();

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonProperty("user_id")
    private User user;

    @Column(name = "token", nullable = false, unique = true, length = 500)
    @JsonProperty("token")
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 20)
    @JsonProperty("platform")
    private DevicePlatform platform;

    @Column(name = "device_id", length = 100)
    @JsonProperty("device_id")
    private String deviceId;

    @Column(name = "app_version", length = 50)
    @JsonProperty("app_version")
    private String appVersion;

    @Column(name = "is_active", nullable = false)
    @JsonProperty("is_active")
    private Boolean isActive = true;

    @Column(name = "last_used_at")
    @JsonProperty("last_used_at")
    private OffsetDateTime lastUsedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (lastUsedAt == null) {
            lastUsedAt = OffsetDateTime.now();
        }
    }

    public void updateLastUsed() {
        this.lastUsedAt = OffsetDateTime.now();
    }

    public void deactivate() {
        this.isActive = false;
    }
}
