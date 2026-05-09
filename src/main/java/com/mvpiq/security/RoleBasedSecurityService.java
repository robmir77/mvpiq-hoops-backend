package com.mvpiq.security;

import com.mvpiq.enums.UserRole;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
@Slf4j
public class RoleBasedSecurityService {

    @Inject
    SecurityIdentity securityIdentity;

    public boolean hasRole(UserRole role) {
        return securityIdentity.hasRole(role.name());
    }

    public boolean hasAnyRole(UserRole... roles) {
        return Arrays.stream(roles)
                .anyMatch(role -> securityIdentity.hasRole(role.name()));
    }

    public boolean hasAllRoles(UserRole... roles) {
        return Arrays.stream(roles)
                .allMatch(role -> securityIdentity.hasRole(role.name()));
    }

    public boolean isCreator() {
        return Boolean.TRUE.equals(securityIdentity.getAttribute("is_creator"));
    }

    public boolean isTrainer() {
        return Boolean.TRUE.equals(securityIdentity.getAttribute("is_trainer"));
    }

    public boolean isOwner(UUID resourceOwnerId) {
        if (resourceOwnerId == null) {
            return false;
        }
        
        String currentUserId = securityIdentity.getAttribute("user_id");
        if (currentUserId == null) {
            return false;
        }
        
        try {
            UUID currentUuid = UUID.fromString(currentUserId);
            return currentUuid.equals(resourceOwnerId);
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format for current user ID: {}", currentUserId);
            return false;
        }
    }

    public UUID getCurrentUserId() {
        String userIdStr = securityIdentity.getAttribute("user_id");
        if (userIdStr == null) {
            return null;
        }
        
        try {
            return UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format for current user ID: {}", userIdStr);
            return null;
        }
    }

    public String getCurrentUsername() {
        return securityIdentity.getAttribute("username");
    }

    public String getCurrentEmail() {
        return securityIdentity.getAttribute("email");
    }

    public boolean canAccessResource(UUID resourceOwnerId, UserRole... allowedRoles) {
        // Admin can access everything
        if (hasRole(UserRole.admin)) {
            return true;
        }
        
        // Owner can access their own resources
        if (isOwner(resourceOwnerId)) {
            return true;
        }
        
        // Check if user has any of the allowed roles
        return hasAnyRole(allowedRoles);
    }

    public boolean canScout() {
        return hasRole(UserRole.scout) || 
               hasRole(UserRole.admin) || 
               hasRole(UserRole.trainer);
    }

    public boolean canTrain() {
        return hasRole(UserRole.trainer) || 
               hasRole(UserRole.admin) || 
               isTrainer();
    }

    public boolean canCreateContent() {
        return hasRole(UserRole.creator) || 
               hasRole(UserRole.admin) || 
               isCreator();
    }

    public boolean canManageUsers() {
        return hasRole(UserRole.admin);
    }

    public Set<String> getCurrentRoles() {
        return securityIdentity.getRoles();
    }

    public boolean isAnonymous() {
        return securityIdentity.isAnonymous();
    }

    public UserSecurityInfo getCurrentUserSecurityInfo() {
        return UserSecurityInfo.builder()
                .userId(getCurrentUserId())
                .username(getCurrentUsername())
                .email(getCurrentEmail())
                .roles(getCurrentRoles())
                .isCreator(isCreator())
                .isTrainer(isTrainer())
                .build();
    }

    @lombok.Builder
    @lombok.Data
    public static class UserSecurityInfo {
        private UUID userId;
        private String username;
        private String email;
        private Set<String> roles;
        private Boolean isCreator;
        private Boolean isTrainer;
    }
}
