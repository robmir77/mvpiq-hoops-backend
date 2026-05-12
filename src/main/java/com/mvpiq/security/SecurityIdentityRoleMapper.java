package com.mvpiq.security;

import com.mvpiq.enums.UserRole;
import com.mvpiq.model.UserRoleAssignment;
import com.mvpiq.repositories.UserRoleRepository;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class SecurityIdentityRoleMapper {

    @Inject
    UserRoleRepository userRoleRepository;

    public Uni<SecurityIdentity> map(SecurityIdentity identity, AuthenticationRequestContext context) {
        if (identity.isAnonymous()) {
            return Uni.createFrom().item(identity);
        }

        JsonWebToken jwt = (JsonWebToken) identity.getPrincipal();
        Set<String> roles = new HashSet<>();

        // Get user ID from JWT
        String userIdStr = jwt.getClaim(Claims.sub);
        UUID userId = UUID.fromString(userIdStr);

        // Fetch user roles from database using RBAC system
        List<UserRoleAssignment> userRoles = userRoleRepository.findByUserId(userId);
        
        // Convert role codes to role names
        for (UserRoleAssignment ur : userRoles) {
            String roleCode = ur.getRole().getCode();
            try {
                UserRole userRoleEnum = UserRole.valueOf(roleCode);
                roles.add(userRoleEnum.name());
            } catch (IllegalArgumentException e) {
                // Invalid role, skip
            }
        }

        // If no roles found, default to player
        if (roles.isEmpty()) {
            roles.add(UserRole.PLAYER.name());
        }

        // Add role from JWT claim for backward compatibility
        String roleClaim = jwt.getClaim("role");
        if (roleClaim != null && !roles.contains(roleClaim)) {
            try {
                UserRole userRole = UserRole.valueOf(roleClaim.toUpperCase());
                roles.add(userRole.name());
            } catch (IllegalArgumentException e) {
                // Invalid role, ignore
            }
        }

        QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder()
                .setPrincipal(new QuarkusPrincipal(jwt.getName()))
                .addRoles(roles)
                .addAttribute("user_id", userIdStr)
                .addAttribute("username", jwt.getClaim("preferred_username"))
                .addAttribute("email", jwt.getClaim("email"));

        return Uni.createFrom().item(builder.build());
    }
}
