package com.mvpiq.security;

import com.mvpiq.enums.UserRole;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@ApplicationScoped
@Priority(Priorities.AUTHORIZATION)
@Slf4j
public class RoleBasedSecurityFilter implements ContainerRequestFilter {

    @Context
    UriInfo uriInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        SecurityIdentity identity = (SecurityIdentity) requestContext.getSecurityContext().getUserPrincipal();
        
        if (identity == null || identity.isAnonymous()) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\":\"Unauthorized\"}")
                    .build());
            return;
        }

        String path = uriInfo.getPath();
        String method = requestContext.getMethod();

        // Define role-based access rules
        if (!hasRequiredRole(identity, path, method)) {
            requestContext.abortWith(Response.status(Response.Status.FORBIDDEN)
                    .entity("{\"error\":\"Forbidden: Insufficient permissions\"}")
                    .build());
        }
    }

    private boolean hasRequiredRole(SecurityIdentity identity, String path, String method) {
        Set<String> userRoles = identity.getRoles();
        String userId = identity.getAttribute("user_id");

        // Admin has access to everything
        if (userRoles.contains(UserRole.admin.name())) {
            return true;
        }

        // Scout-specific endpoints
        if (path.contains("/api/scout/")) {
            return userRoles.contains(UserRole.scout.name()) || 
                   userRoles.contains(UserRole.admin.name()) ||
                   userRoles.contains(UserRole.trainer.name());
        }

        // Trainer-specific endpoints
        if (path.contains("/api/trainer/")) {
            return userRoles.contains(UserRole.trainer.name()) || 
                   userRoles.contains(UserRole.admin.name()) ||
                   identity.getAttributes().containsKey("is_trainer");
        }

        // Creator-specific endpoints (for exercise creation, etc.)
        if ((path.contains("/api/exercises/") && "POST".equals(method)) ||
            (path.contains("/api/media/") && "POST".equals(method))) {
            return userRoles.contains(UserRole.creator.name()) || 
                   userRoles.contains(UserRole.admin.name()) ||
                   identity.getAttributes().containsKey("is_creator");
        }

        // Player profile access - users can only access their own profile unless public
        if (path.contains("/api/player-profiles/") && !path.contains("/public")) {
            String[] pathParts = path.split("/");
            if (pathParts.length > 3) {
                String profileId = pathParts[3];
                // Allow access if it's the user's own profile or if they have appropriate roles
                return profileId.equals(userId) || 
                       userRoles.contains(UserRole.trainer.name()) ||
                       userRoles.contains(UserRole.scout.name()) ||
                       userRoles.contains(UserRole.admin.name());
            }
        }

        // Conversation access - users can only access conversations they participate in
        if (path.contains("/api/conversations/")) {
            String[] pathParts = path.split("/");
            if (pathParts.length > 3) {
                String conversationId = pathParts[3];
                // This would need to be implemented with actual database check
                // For now, allow authenticated users
                return true;
            }
        }

        // Default: allow authenticated users
        return true;
    }

    private Set<String> getRequiredRoles(String path, String method) {
        Set<String> requiredRoles = new HashSet<>();

        // Define specific role requirements for different endpoints
        if (path.contains("/api/scout/")) {
            requiredRoles.add(UserRole.scout.name());
        }

        if (path.contains("/api/trainer/")) {
            requiredRoles.add(UserRole.trainer.name());
        }

        if ((path.contains("/api/exercises/") && "POST".equals(method)) ||
            (path.contains("/api/media/") && "POST".equals(method))) {
            requiredRoles.add(UserRole.creator.name());
        }

        return requiredRoles;
    }
}
