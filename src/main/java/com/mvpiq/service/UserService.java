package com.mvpiq.service;

import com.mvpiq.dto.RegisterDTO;
import com.mvpiq.enums.UserRole;
import com.mvpiq.model.Player;
import com.mvpiq.model.User;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.*;

@ApplicationScoped
public class UserService {

    private User createUserFromRole(UserRole role, RegisterDTO dto, String hash) {
        if (role == UserRole.player) {
            Player p = new Player();
            p.setRole(UserRole.player);
            p.setUsername(dto.getUsername());
            p.setEmail(dto.getEmail());
            p.setDisplayName(dto.getDisplayName());
            p.setPasswordHash(hash);
            return p;
        }

        User u = new User();
        u.setRole(role);
        u.setUsername(dto.getUsername());
        u.setEmail(dto.getEmail());
        u.setDisplayName(dto.getDisplayName());
        u.setPasswordHash(hash);
        return u;
    }
}