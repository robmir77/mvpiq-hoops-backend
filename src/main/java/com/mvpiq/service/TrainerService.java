package com.mvpiq.service;

import com.mvpiq.model.*;
import com.mvpiq.repositories.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@ApplicationScoped
public class TrainerService {

    @Inject
    TrainerFollowRepository trainerFollowRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    UserRoleRepository userRoleRepository;

    @Inject
    TrainerFeedbackRepository trainerFeedbackRepository;

    @Inject
    PlayerRepository playerRepository;

    @Inject
    AthleteGoalRepository athleteGoalRepository;

    @Inject
    WorkoutSessionRepository workoutSessionRepository;

    @Inject
    ProgressTrackingService progressTrackingService;

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
        List<Map<String, Object>> progressList = new ArrayList<>();
        
        for (TrainerFollows f : follows) {
            User player = f.getPlayer();
            ProgressTrackingService.ProgressSummary summary = progressTrackingService.getProgressSummary(player.getId());
            
            Map<String, Object> playerProgress = new HashMap<>();
            playerProgress.put("playerId", player.getId());
            playerProgress.put("displayName", player.getDisplayName());
            playerProgress.put("avatarUrl", player.getAvatarUrl());
            playerProgress.put("totalSessions", summary.getTotalSessions());
            playerProgress.put("completedGoals", summary.getCompletedGoals());
            playerProgress.put("activeGoals", summary.getActiveGoals());
            playerProgress.put("totalPoints", summary.getTotalPoints());
            playerProgress.put("currentStreak", summary.getCurrentStreak());
            playerProgress.put("weeklySessions", summary.getWeeklySessions());
            playerProgress.put("weeklyMinutes", summary.getWeeklyMinutes());
            
            progressList.add(playerProgress);
        }
        
        return progressList;
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
        
        Player player = playerRepository.findById(playerId);
        if (player == null) {
            return Optional.empty();
        }
        
        ProgressTrackingService.ProgressSummary summary = progressTrackingService.getProgressSummary(playerId);
        List<AthleteGoal> activeGoals = athleteGoalRepository.findActiveGoals(playerId);
        List<TrainerFeedback> feedbackHistory = trainerFeedbackRepository.findByPlayerId(playerId);
        List<WorkoutSession> recentWorkouts = workoutSessionRepository.findByPlayerAndStatus(playerId, "COMPLETED");
        if (recentWorkouts.size() > 5) {
            recentWorkouts = recentWorkouts.subList(0, 5);
        }
        
        Map<String, Object> details = new HashMap<>();
        details.put("playerId", player.getId());
        details.put("username", player.getUsername());
        details.put("displayName", player.getDisplayName());
        details.put("avatarUrl", player.getAvatarUrl());
        details.put("birthDate", player.getBirthDate());
        details.put("heightCm", player.getHeightCm());
        details.put("weightKg", player.getWeightKg());
        details.put("wingspanCm", player.getWingspanCm());
        details.put("verticalJumpCm", player.getVerticalJumpCm());
        details.put("level", player.getLevel());
        details.put("dominantHand", player.getDominantHand());
        details.put("country", player.getCountry());
        details.put("city", player.getCity());
        
        if (player.getPreferredPosition() != null) {
            details.put("preferredPosition", player.getPreferredPosition().getLabel());
        }
        
        details.put("progress", summary);
        details.put("activeGoals", activeGoals);
        details.put("feedbackHistory", feedbackHistory);
        details.put("recentWorkouts", recentWorkouts);
        
        return Optional.of(details);
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
        
        User trainer = userRepository.findById(trainerId);
        if (trainer == null) {
            throw new IllegalArgumentException("Trainer not found");
        }
        
        User player = userRepository.findById(playerId);
        if (player == null) {
            throw new IllegalArgumentException("Player not found");
        }
        
        TrainerFeedback tf = new TrainerFeedback();
        tf.setTrainer(trainer);
        tf.setPlayer(player);
        tf.setFeedback(feedback);
        tf.setCreatedAt(OffsetDateTime.now());
        
        trainerFeedbackRepository.persist(tf);
        log.info("Feedback persisted successfully: {}", feedback);
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
