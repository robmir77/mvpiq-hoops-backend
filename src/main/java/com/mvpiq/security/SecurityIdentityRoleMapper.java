package com.mvpiq.security;

import com.mvpiq.enums.UserRole;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@ApplicationScoped
public class SecurityIdentityRoleMapper {

    public Uni<SecurityIdentity> map(SecurityIdentity identity, AuthenticationRequestContext context) {
        if (identity.isAnonymous()) {
            return Uni.createFrom().item(identity);
        }

        JsonWebToken jwt = (JsonWebToken) identity.getPrincipal();
        Set<String> roles = new HashSet<>();

        // Add role from claim
        String roleClaim = jwt.getClaim("role");
        if (roleClaim != null) {
            try {
                UserRole userRole = UserRole.valueOf(roleClaim.toLowerCase());
                roles.add(userRole.name());
            } catch (IllegalArgumentException e) {
                // Invalid role, use default
                roles.add(UserRole.player.name());
            }
        } else {
            // Default role if not specified
            roles.add(UserRole.player.name());
        }

        // Add additional role flags
        Boolean isCreator = jwt.getClaim("is_creator");
        Boolean isTrainer = jwt.getClaim("is_trainer");

        if (Boolean.TRUE.equals(isCreator)) {
            roles.add("creator");
        }
        if (Boolean.TRUE.equals(isTrainer)) {
            roles.add("trainer");
        }

        QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder()
                .setPrincipal(new QuarkusPrincipal(jwt.getName()))
                .addRoles(roles)
                .addAttribute("user_id", jwt.getClaim(Claims.sub))
                .addAttribute("username", jwt.getClaim("preferred_username"))
                .addAttribute("email", jwt.getClaim("email"))
                .addAttribute("is_creator", isCreator)
                .addAttribute("is_trainer", isTrainer);

        return Uni.createFrom().item(builder.build());
    }
}
