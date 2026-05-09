package com.mvpiq.service;

import com.mvpiq.model.*;
import com.mvpiq.repositories.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@ApplicationScoped
public class AchievementService {

    @Inject
    BadgeRepository badgeRepository;
    
    @Inject
    AthleteBadgeRepository athleteBadgeRepository;
    
    @Inject
    TrainingSessionRepository trainingSessionRepository;
    
    @Inject
    UserActivityLogRepository activityLogRepository;
    
    @Inject
    AthletePointsRepository athletePointsRepository;
    
    @Inject
    PlayerProfileRepository playerProfileRepository;

    /**
     * Controlla e assegna achievement per un utente dopo un'azione
     */
    @Transactional
    public void checkAndAwardAchievements(UUID userId, String activityType) {
        log.info("Checking achievements for user {} after activity: {}", userId, activityType);
        
        // Recupera tutti i badge disponibili
        List<Badge> allBadges = badgeRepository.findAll().list();
        
        // Recupera badge già ottenuti
        List<AthleteBadge> earnedBadges = athleteBadgeRepository.findByPlayerId(userId);
        List<UUID> earnedBadgeIds = earnedBadges.stream()
                .map(badge -> badge.getBadge().getId())
                .toList();
        
        // Controlla ogni badge non ancora ottenuto
        for (Badge badge : allBadges) {
            if (!earnedBadgeIds.contains(badge.getId())) {
                if (checkAchievementCriteria(userId, badge, activityType)) {
                    awardBadge(userId, badge.getId());
                }
            }
        }
    }

    /**
     * Assegna un badge specifico a un utente
     */
    @Transactional
    public void awardBadge(UUID userId, UUID badgeId) {
        Optional<Badge> badgeOpt = badgeRepository.findByIdOptional(badgeId);
        if (badgeOpt.isEmpty()) {
            log.warn("Badge not found: {}", badgeId);
            return;
        }
        
        Badge badge = badgeOpt.get();
        
        // Verifica se il badge è già stato ottenuto
        boolean alreadyEarned = athleteBadgeRepository.existsByPlayerIdAndBadgeId(userId, badgeId);
        if (alreadyEarned) {
            log.info("User {} already has badge {}", userId, badge.getCode());
            return;
        }
        
        // Assegna il badge
        AthleteBadge athleteBadge = new AthleteBadge();
        
        // Recupera il PlayerProfile per l'utente
        PlayerProfile playerProfile = playerProfileRepository.findByUserId(userId);
        if (playerProfile == null) {
            log.warn("PlayerProfile not found for user: {}", userId);
            return;
        }
        
        athleteBadge.setPlayer(playerProfile);
        athleteBadge.setBadge(badge);
        athleteBadge.setObtainedAt(OffsetDateTime.now());
        
        athleteBadgeRepository.persist(athleteBadge);
        
        // Aggiorna i punti totali
        updatePlayerPoints(userId, badge.getPoints());
        
        // Log dell'achievement
        UserActivityLog achievementLog = new UserActivityLog();
        achievementLog.setUserId(userId);
        achievementLog.setActivityType("ACHIEVEMENT_EARNED");
        achievementLog.setMetadata(String.format("{\"badge_code\":\"%s\",\"badge_name\":\"%s\",\"points\":%d}", 
                badge.getCode(), badge.getName(), badge.getPoints()));
        achievementLog.setCreatedAt(OffsetDateTime.now());
        
        activityLogRepository.persist(achievementLog);
        
        log.info("Awarded badge {} to user {} (+{} points)", badge.getCode(), userId, badge.getPoints());
    }

    /**
     * Verifica se l'utente soddisfa i criteri per un achievement
     */
    private boolean checkAchievementCriteria(UUID userId, Badge badge, String activityType) {
        String badgeCode = badge.getCode();
        
        switch (badgeCode) {
            case "FIRST_LOGIN":
                return "LOGIN".equals(activityType);
            
            case "FIRST_WORKOUT":
                return "TRAINING_SESSION".equals(activityType) && 
                       trainingSessionRepository.countByUserId(userId) >= 1;
            
            case "WEEK_STREAK":
                return getStreakDays(userId) >= 7;
            
            case "MONTH_STREAK":
                return getStreakDays(userId) >= 30;
            
            case "TEN_WORKOUTS":
                return trainingSessionRepository.countByUserId(userId) >= 10;
            
            case "HUNDRED_WORKOUTS":
                return trainingSessionRepository.countByUserId(userId) >= 100;
            
            case "FIVE_BADGES":
                return athleteBadgeRepository.countByPlayerId(userId) >= 5;
            
            case "TEN_BADGES":
                return athleteBadgeRepository.countByPlayerId(userId) >= 10;
            
            case "VIDEO_ANALYSIS_FIRST":
                return "VIDEO_ANALYSIS".equals(activityType) &&
                       hasVideoAnalysis(userId);
            
            case "VIDEO_ANALYSIS_TEN":
                return getVideoAnalysisCount(userId) >= 10;
            
            case "POINTS_HUNDRED":
                return getPlayerPoints(userId) >= 100;
            
            case "POINTS_THOUSAND":
                return getPlayerPoints(userId) >= 1000;
            
            case "GOAL_FIRST":
                return "GOAL_COMPLETED".equals(activityType) &&
                       hasCompletedGoals(userId);
            
            case "GOAL_TEN":
                return getCompletedGoalsCount(userId) >= 10;
            
            default:
                return false;
        }
    }

    /**
     * Ottiene i badge di un utente
     */
    public List<AthleteBadge> getUserBadges(UUID userId) {
        return athleteBadgeRepository.findByPlayerId(userId);
    }

    /**
     * Ottiene i badge disponibili per categoria
     */
    public List<Badge> getBadgesByCategory(String category) {
        if (category == null || category.isEmpty()) {
            return badgeRepository.findAll().list();
        }
        return badgeRepository.findByCategory(category);
    }

    /**
     * Crea badge di sistema se non esistono
     */
    @Transactional
    public void initializeSystemBadges() {
        log.info("Initializing system badges");
        
        Badge[] systemBadges = {
            createBadge("FIRST_LOGIN", "Primo Accesso", "Hai effettuato il primo login", 10, "login"),
            createBadge("FIRST_WORKOUT", "Primo Allenamento", "Hai completato il primo allenamento", 25, "training"),
            createBadge("WEEK_STREAK", "Settimana da Campione", "7 giorni consecutivi di allenamento", 50, "streak"),
            createBadge("MONTH_STREAK", "Mese da Campione", "30 giorni consecutivi di allenamento", 200, "streak"),
            createBadge("TEN_WORKOUTS", "Dieci Allenamenti", "Hai completato 10 allenamenti", 100, "training"),
            createBadge("HUNDRED_WORKOUTS", "Centenario", "Hai completato 100 allenamenti", 500, "training"),
            createBadge("FIVE_BADGES", "Collezionista", "Hai ottenuto 5 badge", 30, "achievement"),
            createBadge("TEN_BADGES", "Super Collezionista", "Hai ottenuto 10 badge", 75, "achievement"),
            createBadge("VIDEO_ANALYSIS_FIRST", "Prima Analisi", "Hai analizzato il tuo primo video", 40, "video"),
            createBadge("VIDEO_ANALYSIS_TEN", "Analista Esperto", "Hai analizzato 10 video", 150, "video"),
            createBadge("POINTS_HUNDRED", "Centenario Punti", "Hai raggiunto 100 punti", 20, "points"),
            createBadge("POINTS_THOUSAND", "Milla Punti", "Hai raggiunto 1000 punti", 100, "points"),
            createBadge("GOAL_FIRST", "Primo Obiettivo", "Hai completato il primo obiettivo", 35, "goals"),
            createBadge("GOAL_TEN", "Obiettivi da Campione", "Hai completato 10 obiettivi", 120, "goals")
        };
        
        for (Badge badge : systemBadges) {
            if (!badgeRepository.existsByCode(badge.getCode())) {
                badgeRepository.persist(badge);
                log.info("Created system badge: {}", badge.getCode());
            }
        }
    }

    /**
     * Aggiorna i punti di un utente
     */
    private void updatePlayerPoints(UUID userId, int additionalPoints) {
        Optional<AthletePoints> existingPoints = athletePointsRepository.findByPlayer(userId);
        if (existingPoints.isPresent()) {
            AthletePoints points = existingPoints.get();
            points.setTotalPoints(points.getTotalPoints() + additionalPoints);
            points.setUpdatedAt(OffsetDateTime.now());
        } else {
            PlayerProfile player = playerProfileRepository.findByUserId(userId);
            if (player == null) {
                throw new IllegalArgumentException("Player not found: " + userId);
            }
            AthletePoints points = new AthletePoints();
            points.setPlayer(player);
            points.setTotalPoints((long) additionalPoints);
            points.setUpdatedAt(OffsetDateTime.now());
            athletePointsRepository.persist(points);
        }
    }

    /**
     * Helper methods
     */
    private int getStreakDays(UUID userId) {
        return (int) activityLogRepository.countByUserIdAndActivityType(userId, "DAILY_PROGRESS");
    }

    private boolean hasVideoAnalysis(UUID userId) {
        return videoAnalysisSessionRepository.countByUserId(userId) > 0;
    }

    private int getVideoAnalysisCount(UUID userId) {
        return (int) videoAnalysisSessionRepository.countByUserId(userId);
    }

    private long getPlayerPoints(UUID userId) {
        Optional<AthletePoints> points = athletePointsRepository.findByPlayer(userId);
        return points.map(AthletePoints::getTotalPoints).orElse(0L);
    }

    private boolean hasCompletedGoals(UUID userId) {
        return athleteGoalRepository.countByPlayerIdAndStatus(userId, "COMPLETED") > 0;
    }

    private long getCompletedGoalsCount(UUID userId) {
        return athleteGoalRepository.countByPlayerIdAndStatus(userId, "COMPLETED");
    }

    private Badge createBadge(String code, String name, String description, int points, String category) {
        Badge badge = new Badge();
        badge.setCode(code);
        badge.setName(name);
        badge.setDescription(description);
        badge.setPoints(points);
        badge.setCategory(category);
        badge.setCreatedAt(OffsetDateTime.now());
        return badge;
    }

    // Repository injection aggiuntivi
    @Inject
    VideoAnalysisSessionRepository videoAnalysisSessionRepository;
    
    @Inject
    AthleteGoalRepository athleteGoalRepository;
}
