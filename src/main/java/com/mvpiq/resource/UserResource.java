package com.mvpiq.resource;

import com.mvpiq.dto.UserDTO;
import com.mvpiq.model.User;
import com.mvpiq.model.UserActivityLog;
import com.mvpiq.repositories.UserActivityLogRepository;
import com.mvpiq.repositories.UserRepository;
import com.mvpiq.security.RoleBasedSecurityService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Path("/api")
@RequestScoped
public class UserResource {

    @Inject
    UserRepository userRepository;

    @Inject
    UserActivityLogRepository activityLogRepository;

    @Inject
    RoleBasedSecurityService securityService;

    // --- Recupera i dati dell'utente corrente ---
    @GET
    @Path("/users/me/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCurrentUser(@PathParam("userId") UUID userId) {
        if (userId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Missing userId parameter").build();
        }

        return userRepository.findByIdOptional(userId)
                .map(UserDTO::fromEntity)
                .map(dto -> Response.ok(dto).build())
                .orElse(Response.status(Response.Status.NOT_FOUND)
                        .entity("User not found").build());
    }

    /**
     * GET /api/users/online
     * Recupera gli utenti con attività recente (online) - solo admin
     */
    @GET
    @Path("/users/online")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOnlineUsers(@QueryParam("minutesAgo") @DefaultValue("15") Integer minutesAgo) {
        if (!securityService.canManageUsers()) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        List<UserActivityLog> recentActivities = activityLogRepository.findUniqueUsersWithRecentActivity(minutesAgo);

        // Raggruppa per userId e prendi l'attività più recente per ogni utente
        Map<UUID, UserActivityLog> latestActivityByUser = new HashMap<>();
        for (UserActivityLog activity : recentActivities) {
            UserActivityLog existing = latestActivityByUser.get(activity.getUserId());
            if (existing == null || activity.getCreatedAt().isAfter(existing.getCreatedAt())) {
                latestActivityByUser.put(activity.getUserId(), activity);
            }
        }

        // Recupera i dettagli degli utenti
        List<OnlineUserDTO> onlineUsers = latestActivityByUser.entrySet().stream()
                .map(entry -> {
                    UUID userId = entry.getKey();
                    UserActivityLog latestActivity = entry.getValue();
                    User user = userRepository.findById(userId);
                    if (user == null) return null;

                    return OnlineUserDTO.builder()
                            .userId(user.getId())
                            .username(user.getUsername())
                            .email(user.getEmail())
                            .displayName(user.getDisplayName())
                            .avatarUrl(user.getAvatarUrl())
                            .lastActivityAt(latestActivity.getCreatedAt())
                            .activityType(latestActivity.getActivityType())
                            .build();
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(OnlineUserDTO::getLastActivityAt).reversed())
                .collect(Collectors.toList());

        return Response.ok(onlineUsers).build();
    }

    @lombok.Builder
    @lombok.Data
    public static class OnlineUserDTO {
        private UUID userId;
        private String username;
        private String email;
        private String displayName;
        private String avatarUrl;
        private OffsetDateTime lastActivityAt;
        private String activityType;
    }
}
