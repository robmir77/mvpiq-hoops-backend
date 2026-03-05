package com.mvpiq.service;

import io.smallrye.jwt.build.Jwt;
import com.mvpiq.dto.LoginDTO;
import com.mvpiq.dto.LoginResponseDTO;
import com.mvpiq.dto.RegisterDTO;
import com.mvpiq.enums.UserRole;
import com.mvpiq.model.Player;
import com.mvpiq.model.User;
import com.mvpiq.repositories.PlayerRepository;
import com.mvpiq.repositories.UserRepository;
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

    @Transactional
    public User register(RegisterDTO dto) {

        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new RuntimeException("Email already in use");
        }

        String hashedPassword = PasswordUtils.hashWithSaltString(dto.getPassword());

        UserRole role = dto.getRole() != null ? dto.getRole() : UserRole.player;

        // Se è player → creiamo direttamente Player (eredita da User)
        if (role == UserRole.player) {

            Player player = new Player();
            player.setUsername(dto.getUsername());
            player.setEmail(dto.getEmail());
            player.setDisplayName(dto.getDisplayName());
            player.setRole(UserRole.player);
            player.setPasswordHash(hashedPassword);

            playerRepository.persist(player);

            return player;
        }

        // Altri ruoli → solo User base
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setDisplayName(dto.getDisplayName());
        user.setRole(role);
        user.setPasswordHash(hashedPassword);

        userRepository.persist(user);

        return user;
    }

    // LOGIN
    public LoginResponseDTO login(LoginDTO dto) {

        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!PasswordUtils.verifyWithSaltString(dto.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = Jwt.issuer("mvpiq-hoops")
                .subject(user.getId().toString())
                .claim("role", user.getRole().name())
                .expiresIn(3600)
                .sign();

        return LoginResponseDTO.builder()
                .token(token)
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .role(user.getRole())
                .verified(user.getVerified())
                .build();
    }
}
