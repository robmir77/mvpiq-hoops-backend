package com.mvpiq.service;

import io.smallrye.jwt.build.Jwt;
import com.mvpiq.dto.LoginDTO;
import com.mvpiq.dto.LoginResponseDTO;
import com.mvpiq.dto.RegisterDTO;
import com.mvpiq.enums.UserRole;
import com.mvpiq.model.Player;
import com.mvpiq.model.Role;
import com.mvpiq.model.User;
import com.mvpiq.model.UserRoleAssignment;
import com.mvpiq.repositories.AthleteGoalsRepository;
import com.mvpiq.repositories.PlayerRepository;
import com.mvpiq.repositories.RoleRepository;
import com.mvpiq.repositories.UserRepository;
import com.mvpiq.repositories.UserRoleRepository;
import com.mvpiq.security.PasswordUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class AuthService {

    @Inject
    UserRepository userRepository;

    @Inject
    PlayerRepository playerRepository;

    @Inject
    AthleteGoalsRepository athleteGoalsRepository;

    @Inject
    RoleRepository roleRepository;

    @Inject
    UserRoleRepository userRoleRepository;

    @Transactional
    public LoginResponseDTO register(RegisterDTO dto) {

        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new RuntimeException("Email already in use");
        }

        String hashedPassword = PasswordUtils.hashWithSaltString(dto.getPassword());

        UserRole requestedRole = dto.getRole() != null ? dto.getRole() : UserRole.PLAYER;

        User user;

        // Se è player → creiamo direttamente Player (eredita da User)
        if (requestedRole == UserRole.PLAYER) {

            Player player = new Player();
            player.setUsername(dto.getUsername());
            player.setEmail(dto.getEmail());
            player.setDisplayName(dto.getDisplayName());
            player.setPasswordHash(hashedPassword);

            playerRepository.persist(player);
            user = player;
        } else {
            // Altri ruoli → solo User base
            user = new User();
            user.setUsername(dto.getUsername());
            user.setEmail(dto.getEmail());
            user.setDisplayName(dto.getDisplayName());
            user.setPasswordHash(hashedPassword);

            userRepository.persist(user);
        }

        // Assegna il ruolo usando il nuovo sistema RBAC
        Role role = roleRepository.findByCode(requestedRole.name())
                .orElseThrow(() -> new RuntimeException("Role not found: " + requestedRole.name()));
        
        UserRoleAssignment userRole = new UserRoleAssignment();
        userRole.setUser(user);
        userRole.setRole(role);
        userRoleRepository.persist(userRole);

        // Genera token JWT dopo la registrazione
        String token = Jwt.issuer("mvpiq-hoops")
                .subject(user.getId().toString())
                .claim("role", requestedRole.name())
                .expiresIn(3600)
                .sign();

        // Verifica se l'utente ha già dei goal
        boolean hasGoals = athleteGoalsRepository.countByPlayerId(user.getId()) > 0;

        return LoginResponseDTO.builder()
                .token(token)
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .role(requestedRole)
                .verified(user.getVerified())
                .hasGoals(hasGoals)
                .build();
    }

    // LOGIN
    public LoginResponseDTO login(LoginDTO dto) {

        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!PasswordUtils.verifyWithSaltString(dto.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        // Recupera il ruolo principale dell'utente dal nuovo sistema RBAC
        UserRoleAssignment primaryUserRole = userRoleRepository.findByUserId(user.getId()).stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("User has no roles assigned"));
        
        com.mvpiq.enums.UserRole roleEnum = com.mvpiq.enums.UserRole.valueOf(primaryUserRole.getRole().getCode());

        String token = Jwt.issuer("mvpiq-hoops")
                .subject(user.getId().toString())
                .claim("role", roleEnum.name())
                .expiresIn(3600)
                .sign();

        // Verifica se l'utente ha già dei goal
        boolean hasGoals = athleteGoalsRepository.countByPlayerId(user.getId()) > 0;

        return LoginResponseDTO.builder()
                .token(token)
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .role(roleEnum)
                .verified(user.getVerified())
                .hasGoals(hasGoals)
                .build();
    }

    // LOGOUT
    public void logout() {
        // Con JWT stateless, il logout viene gestito lato client
        // Questo metodo esiste per completezza API e future implementazioni
        // come token blacklist o refresh token invalidation
    }
}
