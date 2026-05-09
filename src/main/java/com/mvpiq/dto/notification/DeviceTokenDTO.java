package com.mvpiq.dto.notification;

import com.mvpiq.enums.DevicePlatform;
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
public class DeviceTokenDTO {
    
    private UUID id;
    private UUID userId;
    private String token;
    private DevicePlatform platform;
    private String deviceId;
    private String appVersion;
    private Boolean isActive;
    private OffsetDateTime lastUsedAt;
    private OffsetDateTime createdAt;

    public static DeviceTokenDTO fromEntity(com.mvpiq.model.DeviceToken deviceToken) {
        if (deviceToken == null) {
            return null;
        }

        return DeviceTokenDTO.builder()
                .id(deviceToken.getId())
                .userId(deviceToken.getUser() != null ? deviceToken.getUser().getId() : null)
                .token(deviceToken.getToken())
                .platform(deviceToken.getPlatform())
                .deviceId(deviceToken.getDeviceId())
                .appVersion(deviceToken.getAppVersion())
                .isActive(deviceToken.getIsActive())
                .lastUsedAt(deviceToken.getLastUsedAt())
                .createdAt(deviceToken.getCreatedAt())
                .build();
    }
}
