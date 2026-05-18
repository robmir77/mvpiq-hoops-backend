package com.mvpiq.service;

import com.mvpiq.model.TrainerFollows;
import com.mvpiq.model.User;
import com.mvpiq.repositories.TrainerFollowRepository;
import com.mvpiq.repositories.UserRepository;
import com.mvpiq.repositories.UserRoleRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@ApplicationScoped
public class TrainerService {

    @Inject
    TrainerFollowRepository trainerFollowRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    UserRoleRepository userRoleRepository;


    @Transactional
    public TrainerFollows followPlayer(UUID trainerId, UUID playerId) {
        log.info("Trainer {} following player {}", trainerId, playerId);
        
        // Check if trainer exists and has trainer role
        User trainer = userRepository.findById(trainerId);
        if (trainer == null) {
            throw new IllegalArgumentException("Trainer not found: " + trainerId);
        }
        
        // Check if user has trainer role using RBAC
        boolean hasTrainerRole = userRoleRepository.findByUserId(trainerId).stream()
                .anyMatch(ur -> "TRAINER".equals(ur.getRole().getCode()));
        
        if (!hasTrainerRole) {
            throw new IllegalArgumentException("User is not a trainer: " + trainerId);
        }
        
        // Check if player exists
        User player = userRepository.findById(playerId);
        if (player == null) {
            throw new IllegalArgumentException("Player not found: " + playerId);
        }
        
        // Check if already following
        if (trainerFollowRepository.existsByTrainerAndPlayer(trainerId, playerId)) {
            throw new IllegalArgumentException("Trainer is already following this player");
        }
        
        TrainerFollows follow = new TrainerFollows();
        follow.setTrainer(trainer);
        follow.setPlayer(player);
        follow.setCreatedAt(OffsetDateTime.now());
        
        trainerFollowRepository.persist(follow);
        log.info("Trainer {} now following player {}", trainerId, playerId);
        return follow;
    }

    @Transactional
    public void unfollowPlayer(UUID trainerId, UUID playerId) {
        log.info("Trainer {} unfollowing player {}", trainerId, playerId);
        
        TrainerFollows follow = trainerFollowRepository.findByTrainerAndPlayer(trainerId, playerId);
        if (follow != null) {
            trainerFollowRepository.delete(follow);
            log.info("Trainer {} unfollowed player {}", trainerId, playerId);
        }
    }

    public List<TrainerFollows> getTrainerFollows(UUID trainerId) {
        return trainerFollowRepository.findByTrainerId(trainerId);
    }

    public List<TrainerFollows> getPlayerFollowers(UUID playerId) {
        return trainerFollowRepository.findByPlayerId(playerId);
    }

    public boolean isFollowingPlayer(UUID trainerId, UUID playerId) {
        return trainerFollowRepository.existsByTrainerAndPlayer(trainerId, playerId);
    }

    public long getTrainerFollowCount(UUID trainerId) {
        return trainerFollowRepository.countByTrainerId(trainerId);
    }

    public long getPlayerFollowerCount(UUID playerId) {
        return trainerFollowRepository.countByPlayerId(playerId);
    }

    /**
     * Get trainer's players with their progress data
     */
    public List<Map<String, Object>> getTrainerPlayersProgress(UUID trainerId) {
        log.info("Getting progress for trainer's players: {}", trainerId);
        
        List<TrainerFollows> follows = trainerFollowRepository.findByTrainerId(trainerId);
        
        // For each followed player, collect their progress data
        // This would include training sessions, goals, achievements, etc.
        return List.of(); // Placeholder - implement actual progress collection
    }

    /**
     * Get detailed player information for trainer
     */
    public Optional<Map<String, Object>> getPlayerDetailsForTrainer(UUID trainerId, UUID playerId) {
        log.info("Getting player {} details for trainer {}", playerId, trainerId);
        
        // Verify trainer follows this player
        if (!trainerFollowRepository.existsByTrainerAndPlayer(trainerId, playerId)) {
            throw new IllegalArgumentException("Trainer is not following this player");
        }
        
        // Collect comprehensive player data
        return Optional.empty(); // Placeholder - implement actual data collection
    }

    /**
     * Add feedback or notes for a player
     */
    @Transactional
    public void addPlayerFeedback(UUID trainerId, UUID playerId, String feedback) {
        log.info("Adding feedback for player {} from trainer {}", playerId, trainerId);
        
        // Verify trainer follows this player
        if (!trainerFollowRepository.existsByTrainerAndPlayer(trainerId, playerId)) {
            throw new IllegalArgumentException("Trainer is not following this player");
        }
        
        // Implement feedback storage (would need a new entity for trainer feedback)
        // For now, log the feedback
        log.info("Feedback stored: {}", feedback);
    }

    /**
     * Get trainer statistics
     */
    public Map<String, Object> getTrainerStats(UUID trainerId) {
        log.info("Getting stats for trainer: {}", trainerId);
        
        long followCount = trainerFollowRepository.countByTrainerId(trainerId);
        
        return Map.of(
            "followedPlayers", followCount,
            "trainerId", trainerId
        );
    }
}
