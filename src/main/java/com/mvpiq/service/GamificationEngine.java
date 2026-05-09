package com.mvpiq.service;

import com.mvpiq.model.*;
import com.mvpiq.repositories.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@ApplicationScoped
public class GamificationEngine {

    @Inject
    UserActivityLogRepository activityLogRepository;
    
    @Inject
    TrainingSessionRepository trainingSessionRepository;
    
    @Inject
    AthleteBadgeRepository athleteBadgeRepository;
    
    @Inject
    BadgeRepository badgeRepository;
    
    @Inject
    AthletePointsRepository athletePointsRepository;
    
    @Inject
    PlayerProfileRepository playerProfileRepository;

    /**
     * Calcola e aggiorna lo streak di allenamento per un utente
     */
    @Transactional
    public void checkStreak(UUID userId) {
        log.info("Checking streak for user: {}", userId);
        
        // Recupera le sessioni di training degli ultimi 30 giorni
        OffsetDateTime thirtyDaysAgo = OffsetDateTime.now().minusDays(30);
        List<TrainingSession> recentSessions = trainingSessionRepository
                .findByUserIdAndSessionDateAfterOrderBySessionDateDesc(userId, thirtyDaysAgo);
        
        if (recentSessions.isEmpty()) {
            log.info("No training sessions found for user: {}", userId);
            return;
        }
        
        // Calcola lo streak corrente
        int currentStreak = calculateCurrentStreak(recentSessions);
        
        // Salva lo streak come activity log
        UserActivityLog streakLog = new UserActivityLog();
        streakLog.setUserId(userId);
        streakLog.setActivityType("STREAK_UPDATE");
        streakLog.setMetadata("{\"current_streak\": " + currentStreak + "}");
        streakLog.setCreatedAt(OffsetDateTime.now());
        
        activityLogRepository.persist(streakLog);
        
        log.info("Updated streak for user {}: {}", userId, currentStreak);
    }

    /**
     * Calcola i progressi giornalieri per un utente
     */
    @Transactional
    public void calculateDailyProgress(UUID userId) {
        log.info("Calculating daily progress for user: {}", userId);
        
        LocalDate today = LocalDate.now();
        OffsetDateTime startOfDay = today.atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
        OffsetDateTime endOfDay = today.atTime(23, 59, 59).atOffset(OffsetDateTime.now().getOffset());
        
        // Sessioni di oggi
        List<TrainingSession> todaySessions = trainingSessionRepository
                .findByUserIdAndSessionDateBetweenOrderBySessionDateDesc(userId, startOfDay, endOfDay);
        
        // Calcola metriche giornaliere
        int sessionsCount = todaySessions.size();
        int totalDuration = todaySessions.stream()
                .mapToInt(session -> session.getDurationSeconds() != null ? session.getDurationSeconds() : 0)
                .sum();
        
        // Salva progress giornaliero
        UserActivityLog progressLog = new UserActivityLog();
        progressLog.setUserId(userId);
        progressLog.setActivityType("DAILY_PROGRESS");
        progressLog.setMetadata(String.format("{\"date\":\"%s\",\"sessions\":%d,\"duration_seconds\":%d}", 
                today, sessionsCount, totalDuration));
        progressLog.setCreatedAt(OffsetDateTime.now());
        
        activityLogRepository.persist(progressLog);
        
        log.info("Daily progress for user {}: {} sessions, {} seconds", userId, sessionsCount, totalDuration);
    }

    /**
     * Assegna achievement basati sui progressi dell'utente
     */
    @Transactional
    public void awardAchievements(UUID userId) {
        log.info("Checking achievements for user: {}", userId);
        
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
                if (checkAchievementCriteria(userId, badge)) {
                    awardBadge(userId, badge);
                }
            }
        }
    }

    /**
     * Aggiorna i punti totali di un utente
     */
    @Transactional
    public Optional<AthletePoints> updatePlayerPoints(UUID userId) {
        log.info("Updating points for user: {}", userId);
        
        // Calcola punti da badge
        List<AthleteBadge> badges = athleteBadgeRepository.findByPlayerId(userId);
        int badgePoints = badges.stream()
                .mapToInt(badge -> badge.getBadge().getPoints())
                .sum();
        
        // Calcola punti da sessioni di training
        List<TrainingSession> sessions = trainingSessionRepository.findByUserId(userId);
        int sessionPoints = sessions.size() * 10; // 10 punti per sessione
        
        int totalPoints = badgePoints + sessionPoints;
        
        // Aggiorna o crea athlete points
        AthletePoints existingPoints = athletePointsRepository.findById(userId);
        if (existingPoints != null) {
            existingPoints.setTotalPoints((long) totalPoints);
            existingPoints.setUpdatedAt(OffsetDateTime.now());
            log.info("Updated points for user {}: {}", userId, totalPoints);
            return Optional.of(existingPoints);
        } else {
            // Recupero il PlayerProfile esistente
            PlayerProfile player = playerProfileRepository.findById(userId);
            if (player == null) {
                log.warn("PlayerProfile not found for user: {}", userId);
                return Optional.empty();
            }
            
            AthletePoints points = new AthletePoints();
            points.setPlayer(player);
            points.setTotalPoints((long) totalPoints);
            points.setUpdatedAt(OffsetDateTime.now());
            athletePointsRepository.persist(points);
            log.info("Updated points for user {}: {}", userId, totalPoints);
            return Optional.of(points);
        }
    }

    /**
     * Calcola lo streak corrente basato sulle sessioni recenti
     */
    private int calculateCurrentStreak(List<TrainingSession> sessions) {
        if (sessions.isEmpty()) return 0;
        
        LocalDate today = LocalDate.now();
        int streak = 0;
        LocalDate checkDate = today;
        
        for (TrainingSession session : sessions) {
            LocalDate sessionDate = session.getSessionDate().toLocalDate();
            
            // Se la sessione è del giorno che stiamo controllando
            if (sessionDate.equals(checkDate)) {
                streak++;
                checkDate = checkDate.minusDays(1);
            } else if (sessionDate.isBefore(checkDate)) {
                // Se abbiamo trovato una sessione di un giorno precedente, continuiamo a controllare
                continue;
            } else {
                // Se c'è un buco nello streak, fermiamoci
                break;
            }
        }
        
        return streak;
    }

    /**
     * Verifica se l'utente soddisfa i criteri per un achievement
     */
    private boolean checkAchievementCriteria(UUID userId, Badge badge) {
        String badgeCode = badge.getCode();
        
        switch (badgeCode) {
            case "FIRST_LOGIN":
                return true; // Già loggato
            
            case "FIRST_WORKOUT":
                return trainingSessionRepository.countByUserId(userId) >= 1;
            
            case "WEEK_STREAK":
                return getStreakDays(userId) >= 7;
            
            case "MONTH_STREAK":
                return getStreakDays(userId) >= 30;
            
            case "TEN_WORKOUTS":
                return trainingSessionRepository.countByUserId(userId) >= 10;
            
            case "HUNDRED_WORKOUTS":
                return trainingSessionRepository.countByUserId(userId) >= 100;
            
            default:
                return false;
        }
    }

    /**
     * Assegna un badge a un utente
     */
    private void awardBadge(UUID userId, Badge badge) {
        // Recupero il PlayerProfile esistente
        PlayerProfile player = playerProfileRepository.findByUserId(userId);
        if (player == null) {
            log.warn("PlayerProfile not found for user: {}", userId);
            return;
        }
        
        AthleteBadge athleteBadge = new AthleteBadge();
        athleteBadge.setPlayer(player);
        athleteBadge.setBadge(badge);
        athleteBadge.setObtainedAt(OffsetDateTime.now());
        
        athleteBadgeRepository.persist(athleteBadge);
        
        // Aggiorna i punti
        updatePlayerPoints(userId);
        
        log.info("Awarded badge {} to user {}", badge.getCode(), userId);
    }

    /**
     * Ottiene i giorni di streak correnti
     */
    private long getStreakDays(UUID userId) {
        // Implementazione semplificata - basata su activity logs
        return activityLogRepository.countByUserIdAndActivityType(userId, "DAILY_PROGRESS");
    }
}
