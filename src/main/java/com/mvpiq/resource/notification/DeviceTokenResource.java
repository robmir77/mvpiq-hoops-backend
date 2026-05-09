package com.mvpiq.resource.notification;

import com.mvpiq.dto.ApiResponse;
import com.mvpiq.dto.notification.DeviceTokenDTO;
import com.mvpiq.enums.DevicePlatform;
import com.mvpiq.model.DeviceToken;
import com.mvpiq.model.User;
import com.mvpiq.repositories.DeviceTokenRepository;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/api/device-tokens")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Slf4j
public class DeviceTokenResource {

    @Inject
    DeviceTokenRepository deviceTokenRepository;

    @POST
    @Path("/register")
    @Transactional
    public Response registerDeviceToken(DeviceTokenDTO tokenDTO) {
        try {
            // Check if token already exists
            DeviceToken existingToken = deviceTokenRepository.findByToken(tokenDTO.getToken());
            
            if (existingToken != null) {
                // Update existing token
                existingToken.setIsActive(true);
                existingToken.setLastUsedAt(java.time.OffsetDateTime.now());
                existingToken.setAppVersion(tokenDTO.getAppVersion());
                deviceTokenRepository.persist(existingToken);
                
                log.info("Updated existing device token for user: {}", tokenDTO.getUserId());
                return Response.ok(ApiResponse.success(DeviceTokenDTO.fromEntity(existingToken), "Token updated successfully"))
                        .build();
            }

            // Create new token
            DeviceToken deviceToken = DeviceToken.builder()
                    .user(User.builder().id(tokenDTO.getUserId()).build())
                    .token(tokenDTO.getToken())
                    .platform(tokenDTO.getPlatform())
                    .deviceId(tokenDTO.getDeviceId())
                    .appVersion(tokenDTO.getAppVersion())
                    .isActive(true)
                    .build();

            deviceTokenRepository.persist(deviceToken);
            
            log.info("Registered new device token for user: {}", tokenDTO.getUserId());
            return Response.status(Response.Status.CREATED)
                    .entity(ApiResponse.success(DeviceTokenDTO.fromEntity(deviceToken), "Token registered successfully"))
                    .build();

        } catch (Exception e) {
            log.error("Error registering device token", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Failed to register device token: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/user/{userId}")
    public Response getUserDeviceTokens(@PathParam("userId") UUID userId,
                                     @QueryParam("platform") DevicePlatform platform) {
        try {
            List<DeviceToken> tokens;

            if (platform != null) {
                tokens = deviceTokenRepository.findByUserIdAndPlatform(userId, platform);
            } else {
                tokens = deviceTokenRepository.findByUserId(userId);
            }

            List<DeviceTokenDTO> tokenDTOs = tokens.stream()
                    .map(DeviceTokenDTO::fromEntity)
                    .collect(Collectors.toList());

            return Response.ok(ApiResponse.success(tokenDTOs, "Device tokens retrieved successfully"))
                    .build();

        } catch (Exception e) {
            log.error("Error retrieving device tokens for user: {}", userId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Failed to retrieve device tokens: " + e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/deactivate")
    @Transactional
    public Response deactivateDeviceToken(@QueryParam("token") String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(ApiResponse.error("Token parameter is required"))
                        .build();
            }

            DeviceToken deviceToken = deviceTokenRepository.findByToken(token);
            
            if (deviceToken == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(ApiResponse.error("Device token not found"))
                        .build();
            }

            deviceToken.deactivate();
            deviceTokenRepository.persist(deviceToken);
            
            log.info("Deactivated device token: {}", token);
            return Response.ok(ApiResponse.success(null, "Device token deactivated successfully"))
                    .build();

        } catch (Exception e) {
            log.error("Error deactivating device token", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Failed to deactivate device token: " + e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/user/{userId}/deactivate-all")
    @Transactional
    public Response deactivateAllUserTokens(@PathParam("userId") UUID userId) {
        try {
            deviceTokenRepository.deactivateByUserId(userId);
            
            log.info("Deactivated all device tokens for user: {}", userId);
            return Response.ok(ApiResponse.success(null, "All device tokens deactivated successfully"))
                    .build();

        } catch (Exception e) {
            log.error("Error deactivating all device tokens for user: {}", userId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Failed to deactivate all device tokens: " + e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/device/{deviceId}/deactivate")
    @Transactional
    public Response deactivateDeviceByDeviceId(@PathParam("deviceId") String deviceId) {
        try {
            deviceTokenRepository.deactivateByDeviceId(deviceId);
            
            log.info("Deactivated device tokens for device ID: {}", deviceId);
            return Response.ok(ApiResponse.success(null, "Device tokens deactivated successfully"))
                    .build();

        } catch (Exception e) {
            log.error("Error deactivating device tokens for device ID: {}", deviceId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Failed to deactivate device tokens: " + e.getMessage()))
                    .build();
        }
    }

    @DELETE
    @Path("/cleanup")
    @Transactional
    public Response cleanupInactiveTokens() {
        try {
            deviceTokenRepository.deleteInactiveTokens();
            
            log.info("Cleaned up inactive device tokens");
            return Response.ok(ApiResponse.success(null, "Inactive tokens cleaned up successfully"))
                    .build();

        } catch (Exception e) {
            log.error("Error cleaning up inactive tokens", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Failed to cleanup inactive tokens: " + e.getMessage()))
                    .build();
        }
    }
}
