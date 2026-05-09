package com.mvpiq.repositories;

import com.mvpiq.model.DeviceToken;
import com.mvpiq.enums.DevicePlatform;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class DeviceTokenRepository implements PanacheRepositoryBase<DeviceToken, UUID> {

    public List<DeviceToken> findByUserId(UUID userId) {
        return list("user.id = ?1 AND isActive = true", userId);
    }

    public List<DeviceToken> findByUserIdAndPlatform(UUID userId, DevicePlatform platform) {
        return list("user.id = ?1 AND platform = ?2 AND isActive = true", userId, platform);
    }

    public DeviceToken findByToken(String token) {
        return find("token = ?1", token).firstResult();
    }

    public boolean existsByToken(String token) {
        return count("token = ?1", token) > 0;
    }

    public void deactivateByToken(String token) {
        update("isActive = false WHERE token = ?1", token);
    }

    public void deactivateByUserId(UUID userId) {
        update("isActive = false WHERE user.id = ?1", userId);
    }

    public void deactivateByDeviceId(String deviceId) {
        update("isActive = false WHERE deviceId = ?1", deviceId);
    }

    public void deleteInactiveTokens() {
        delete("isActive = false");
    }

    public void updateLastUsed(String token) {
        update("lastUsedAt = CURRENT_TIMESTAMP WHERE token = ?1", token);
    }
}
