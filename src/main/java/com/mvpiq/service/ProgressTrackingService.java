package com.mvpiq.service;

import com.mvpiq.model.*;
import com.mvpiq.repositories.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
public class ProgressTrackingService {

    @Inject
    TrainingSessionRepository trainingSessionRepository;
    
    @Inject
    AthleteGoalsRepository athleteGoalRepository;
    
    @Inject
    UserActivityLogRepository activityLogRepository;
    
    @Inject
    AthletePointsRepository athletePointsRepository;
    
    @Inject
    JournalEntryRepository journalEntryRepository;

    /**
     * Traccia il progresso giornaliero di un utente
     */
    @Transactional
    public void trackDailyProgress(UUID userId) {
        log.info("Tracking daily progress for user: {}", userId);
        
        LocalDate today = LocalDate.now();
        OffsetDateTime startOfDay = today.atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
        OffsetDateTime endOfDay = today.atTime(23, 59, 59).atOffset(OffsetDateTime.now().getOffset());
        
        // Recupera sessioni di oggi
        List<TrainingSession> todaySessions = trainingSessionRepository
                .findByUserIdAndSessionDateBetweenOrderBySessionDateDesc(userId, startOfDay, endOfDay);
        
        // Recupera goal completati oggi
        List<AthleteGoal> completedGoalsToday = athleteGoalRepository
                .findByPlayerIdAndStatusAndCompletedAtBetween(userId, "COMPLETED", startOfDay, endOfDay);
        
        // Calcola metriche
        DailyProgressMetrics metrics = calculateDailyMetrics(todaySessions, completedGoalsToday);
        
        // Salva il progresso giornaliero
        saveDailyProgress(userId, today, metrics);
        
        log.info("Daily progress tracked for user {}: {} sessions, {} goals, {} minutes", 
                userId, metrics.getSessionCount(), metrics.getGoalsCompleted(), metrics.getTotalMinutes());
    }

    /**
     * Calcola le statistiche settimanali
     */
    public WeeklyProgressStats getWeeklyStats(UUID userId) {
        LocalDate weekStart = LocalDate.now().minusDays(7);
        OffsetDateTime weekStartDateTime = weekStart.atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
        OffsetDateTime now = OffsetDateTime.now();
        
        // Sessioni della settimana
        List<TrainingSession> weekSessions = trainingSessionRepository
                .findByUserIdAndSessionDateBetweenOrderBySessionDateDesc(userId, weekStartDateTime, now);
        
        // Goal completati nella settimana
        List<AthleteGoal> weekGoals = athleteGoalRepository
                .findByPlayerIdAndCompletedAtBetween(userId, weekStartDateTime, now);
        
        // Journal entries della settimana
        List<JournalEntry> weekJournals = journalEntryRepository
                .findByPlayerIdAndEntryDateBetweenOrderByEntryDateDesc(userId, weekStartDateTime, now);
        
        return calculateWeeklyStats(weekSessions, weekGoals, weekJournals);
    }

    /**
     * Calcola le statistiche mensili
     */
    public MonthlyProgressStats getMonthlyStats(UUID userId) {
        LocalDate monthStart = LocalDate.now().minusDays(30);
        OffsetDateTime monthStartDateTime = monthStart.atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
        OffsetDateTime now = OffsetDateTime.now();
        
        // Sessioni del mese
        List<TrainingSession> monthSessions = trainingSessionRepository
                .findByUserIdAndSessionDateBetweenOrderBySessionDateDesc(userId, monthStartDateTime, now);
        
        // Goal completati nel mese
        List<AthleteGoal> monthGoals = athleteGoalRepository
                .findByPlayerIdAndCompletedAtBetween(userId, monthStartDateTime, now);
        
        // Calcola trend
        ProgressTrend trend = calculateProgressTrend(userId, 30);
        
        return calculateMonthlyStats(monthSessions, monthGoals, trend);
    }

    /**
     * Ottiene il progresso verso un goal specifico
     */
    public GoalProgress getGoalProgress(UUID goalId) {
        Optional<AthleteGoal> goalOpt = athleteGoalRepository.findByIdOptional(goalId);
        if (goalOpt.isEmpty()) {
            return null;
        }
        
        AthleteGoal goal = goalOpt.get();
        GoalProgress progress = new GoalProgress();
        
        progress.setGoalId(goalId);
        progress.setTitle(goal.getTitle());
        progress.setTargetValue(goal.getTargetValue() != null ? goal.getTargetValue().doubleValue() : null);
        progress.setCurrentValue(goal.getCurrentValue() != null ? goal.getCurrentValue().doubleValue() : null);
        progress.setUnit(goal.getUnit());
        progress.setStatus(goal.getStatus());
        
        // Calcola percentuale di completamento
        if (goal.getTargetValue() != null && goal.getTargetValue().compareTo(BigDecimal.ZERO) > 0) {
            double percentage = (goal.getCurrentValue().doubleValue() / goal.getTargetValue().doubleValue()) * 100;
            progress.setCompletionPercentage(Math.min(100, Math.max(0, percentage)));
        } else {
            progress.setCompletionPercentage(0.0);
        }
        
        // Calcola giorni rimanenti
        if (goal.getDueDate() != null) {
            long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), goal.getDueDate());
            progress.setDaysRemaining((int) daysRemaining);
        } else {
            progress.setDaysRemaining(-1); // Nessuna scadenza
        }
        
        return progress;
    }

    /**
     * Aggiorna il progresso di un goal
     */
    @Transactional
    public void updateGoalProgress(UUID goalId, double newValue) {
        Optional<AthleteGoal> goalOpt = athleteGoalRepository.findByIdOptional(goalId);
        if (goalOpt.isEmpty()) {
            log.warn("Goal not found: {}", goalId);
            return;
        }
        
        AthleteGoal goal = goalOpt.get();
        goal.setCurrentValue(BigDecimal.valueOf(newValue));
        
        // Controlla se il goal è completato
        if (goal.getTargetValue() != null && BigDecimal.valueOf(newValue).compareTo(goal.getTargetValue()) >= 0) {
            goal.setStatus("COMPLETED");
            goal.setCompletedAt(OffsetDateTime.now());
            
            // Log del completamento
            logGoalCompletion(goal.getAthleteId(), goalId);
        }
        
        athleteGoalRepository.persist(goal);
        
        log.info("Updated progress for goal {}: {}/{}", goalId, newValue, goal.getTargetValue());
    }

    /**
     * Ottiene il riepilogo del progresso totale
     */
    public ProgressSummary getProgressSummary(UUID userId) {
        ProgressSummary summary = new ProgressSummary();
        
        // Statistiche generali
        long totalSessions = trainingSessionRepository.countByUserId(userId);
        long completedGoals = athleteGoalRepository.countByPlayerIdAndStatus(userId, "COMPLETED");
        long activeGoals = athleteGoalRepository.countByPlayerIdAndStatus(userId, "ACTIVE");
        
        summary.setTotalSessions(totalSessions);
        summary.setCompletedGoals(completedGoals);
        summary.setActiveGoals(activeGoals);
        
        // Punti totali
        Optional<AthletePoints> pointsOpt = athletePointsRepository.findByIdOptional(userId);
        summary.setTotalPoints(pointsOpt.map(AthletePoints::getTotalPoints).orElse(0L));
        
        // Streak corrente
        summary.setCurrentStreak(getCurrentStreak(userId));
        
        // Progresso settimanale
        WeeklyProgressStats weeklyStats = getWeeklyStats(userId);
        summary.setWeeklySessions(weeklyStats.getSessionCount());
        summary.setWeeklyMinutes(weeklyStats.getTotalMinutes());
        
        return summary;
    }

    /**
     * Calcola le metriche giornaliere
     */
    private DailyProgressMetrics calculateDailyMetrics(List<TrainingSession> sessions, List<AthleteGoal> completedGoals) {
        DailyProgressMetrics metrics = new DailyProgressMetrics();
        
        metrics.setSessionCount(sessions.size());
        metrics.setGoalsCompleted(completedGoals.size());
        metrics.setTotalMinutes(sessions.stream()
                .mapToInt(session -> session.getDurationSeconds() != null ? session.getDurationSeconds() / 60 : 0)
                .sum());
        
        // Calcola calorie stimate (semplificato)
        metrics.setEstimatedCalories(metrics.getTotalMinutes() * 8); // 8 calorie/minuto media
        
        return metrics;
    }

    /**
     * Salva il progresso giornaliero
     */
    private void saveDailyProgress(UUID userId, LocalDate date, DailyProgressMetrics metrics) {
        UserActivityLog progressLog = new UserActivityLog();
        progressLog.setUserId(userId);
        progressLog.setActivityType("DAILY_PROGRESS");
        progressLog.setMetadata(String.format(
                "{\"date\":\"%s\",\"sessions\":%d,\"goals\":%d,\"minutes\":%d,\"calories\":%d}",
                date, metrics.getSessionCount(), metrics.getGoalsCompleted(), 
                metrics.getTotalMinutes(), metrics.getEstimatedCalories()));
        progressLog.setCreatedAt(OffsetDateTime.now());
        
        activityLogRepository.persist(progressLog);
    }

    /**
     * Calcola statistiche settimanali
     */
    private WeeklyProgressStats calculateWeeklyStats(List<TrainingSession> sessions, List<AthleteGoal> goals, List<JournalEntry> journals) {
        WeeklyProgressStats stats = new WeeklyProgressStats();
        
        stats.setSessionCount(sessions.size());
        stats.setGoalsCompleted(goals.size());
        stats.setJournalEntries(journals.size());
        stats.setTotalMinutes(sessions.stream()
                .mapToInt(session -> session.getDurationSeconds() != null ? session.getDurationSeconds() / 60 : 0)
                .sum());
        
        // Giorni con allenamento
        Set<LocalDate> trainingDays = sessions.stream()
                .map(session -> session.getSessionDate().toLocalDate())
                .collect(Collectors.toSet());
        stats.setTrainingDays(trainingDays.size());
        
        return stats;
    }

    /**
     * Calcola statistiche mensili
     */
    private MonthlyProgressStats calculateMonthlyStats(List<TrainingSession> sessions, List<AthleteGoal> goals, ProgressTrend trend) {
        MonthlyProgressStats stats = new MonthlyProgressStats();
        
        stats.setSessionCount(sessions.size());
        stats.setGoalsCompleted(goals.size());
        stats.setTotalMinutes(sessions.stream()
                .mapToInt(session -> session.getDurationSeconds() != null ? session.getDurationSeconds() / 60 : 0)
                .sum());
        
        stats.setProgressTrend(trend);
        
        return stats;
    }

    /**
     * Calcola il trend del progresso
     */
    private ProgressTrend calculateProgressTrend(UUID userId, int days) {
        ProgressTrend trend = new ProgressTrend();
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        
        // Recupera progressi giornalieri
        List<UserActivityLog> dailyLogs = activityLogRepository
                .findByUserIdAndActivityTypeAndCreatedAtBetweenOrderByCreatedAtAsc(
                        userId, "DAILY_PROGRESS", 
                        startDate.atStartOfDay().atOffset(OffsetDateTime.now().getOffset()),
                        endDate.atTime(23, 59, 59).atOffset(OffsetDateTime.now().getOffset()));
        
        if (dailyLogs.size() < 2) {
            trend.setDirection("STABLE");
            trend.setPercentageChange(0.0);
            return trend;
        }
        
        // Confronta prima e ultima settimana
        int firstWeekSessions = extractSessionCount(dailyLogs.get(0));
        int lastWeekSessions = extractSessionCount(dailyLogs.get(dailyLogs.size() - 1));
        
        if (lastWeekSessions > firstWeekSessions) {
            trend.setDirection("IMPROVING");
        } else if (lastWeekSessions < firstWeekSessions) {
            trend.setDirection("DECLINING");
        } else {
            trend.setDirection("STABLE");
        }
        
        // Calcola percentuale di cambio
        double percentageChange = firstWeekSessions > 0 ? 
                ((double)(lastWeekSessions - firstWeekSessions) / firstWeekSessions) * 100 : 0.0;
        trend.setPercentageChange(percentageChange);
        
        return trend;
    }

    /**
     * Log del completamento goal
     */
    private void logGoalCompletion(UUID userId, UUID goalId) {
        UserActivityLog goalLog = new UserActivityLog();
        goalLog.setUserId(userId);
        goalLog.setActivityType("GOAL_COMPLETED");
        goalLog.setMetadata("{\"goal_id\":\"" + goalId + "\"}");
        goalLog.setCreatedAt(OffsetDateTime.now());
        
        activityLogRepository.persist(goalLog);
    }

    /**
     * Ottiene lo streak corrente
     */
    private int getCurrentStreak(UUID userId) {
        // Implementazione semplificata - basata su activity logs
        return (int) activityLogRepository.countByUserIdAndActivityType(userId, "DAILY_PROGRESS");
    }

    /**
     * Estrae il numero di sessioni da un log
     */
    private int extractSessionCount(UserActivityLog activityLog) {
        try {
            // Parsing semplificato del JSON metadata
            String metadata = activityLog.getMetadata();
            if (metadata != null && metadata.contains("\"sessions\":")) {
                int start = metadata.indexOf("\"sessions\":") + 11;
                int end = metadata.indexOf(",", start);
                if (end == -1) end = metadata.indexOf("}", start);
                return Integer.parseInt(metadata.substring(start, end));
            }
        } catch (Exception e) {
            log.warn("Error parsing session count from log: {}", activityLog.getId());
        }
        return 0;
    }

    // DTO classes
    public static class DailyProgressMetrics {
        private int sessionCount;
        private int goalsCompleted;
        private int totalMinutes;
        private int estimatedCalories;

        // Getters and setters
        public int getSessionCount() { return sessionCount; }
        public void setSessionCount(int sessionCount) { this.sessionCount = sessionCount; }
        public int getGoalsCompleted() { return goalsCompleted; }
        public void setGoalsCompleted(int goalsCompleted) { this.goalsCompleted = goalsCompleted; }
        public int getTotalMinutes() { return totalMinutes; }
        public void setTotalMinutes(int totalMinutes) { this.totalMinutes = totalMinutes; }
        public int getEstimatedCalories() { return estimatedCalories; }
        public void setEstimatedCalories(int estimatedCalories) { this.estimatedCalories = estimatedCalories; }
    }

    public static class WeeklyProgressStats {
        private int sessionCount;
        private int goalsCompleted;
        private int journalEntries;
        private int totalMinutes;
        private int trainingDays;

        // Getters and setters
        public int getSessionCount() { return sessionCount; }
        public void setSessionCount(int sessionCount) { this.sessionCount = sessionCount; }
        public int getGoalsCompleted() { return goalsCompleted; }
        public void setGoalsCompleted(int goalsCompleted) { this.goalsCompleted = goalsCompleted; }
        public int getJournalEntries() { return journalEntries; }
        public void setJournalEntries(int journalEntries) { this.journalEntries = journalEntries; }
        public int getTotalMinutes() { return totalMinutes; }
        public void setTotalMinutes(int totalMinutes) { this.totalMinutes = totalMinutes; }
        public int getTrainingDays() { return trainingDays; }
        public void setTrainingDays(int trainingDays) { this.trainingDays = trainingDays; }
    }

    public static class MonthlyProgressStats {
        private int sessionCount;
        private int goalsCompleted;
        private int totalMinutes;
        private ProgressTrend progressTrend;

        // Getters and setters
        public int getSessionCount() { return sessionCount; }
        public void setSessionCount(int sessionCount) { this.sessionCount = sessionCount; }
        public int getGoalsCompleted() { return goalsCompleted; }
        public void setGoalsCompleted(int goalsCompleted) { this.goalsCompleted = goalsCompleted; }
        public int getTotalMinutes() { return totalMinutes; }
        public void setTotalMinutes(int totalMinutes) { this.totalMinutes = totalMinutes; }
        public ProgressTrend getProgressTrend() { return progressTrend; }
        public void setProgressTrend(ProgressTrend progressTrend) { this.progressTrend = progressTrend; }
    }

    public static class ProgressTrend {
        private String direction; // IMPROVING, DECLINING, STABLE
        private double percentageChange;

        // Getters and setters
        public String getDirection() { return direction; }
        public void setDirection(String direction) { this.direction = direction; }
        public double getPercentageChange() { return percentageChange; }
        public void setPercentageChange(double percentageChange) { this.percentageChange = percentageChange; }
    }

    public static class GoalProgress {
        private UUID goalId;
        private String title;
        private Double targetValue;
        private Double currentValue;
        private String unit;
        private String status;
        private double completionPercentage;
        private int daysRemaining;

        // Getters and setters
        public UUID getGoalId() { return goalId; }
        public void setGoalId(UUID goalId) { this.goalId = goalId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public Double getTargetValue() { return targetValue; }
        public void setTargetValue(Double targetValue) { this.targetValue = targetValue; }
        public Double getCurrentValue() { return currentValue; }
        public void setCurrentValue(Double currentValue) { this.currentValue = currentValue; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public double getCompletionPercentage() { return completionPercentage; }
        public void setCompletionPercentage(double completionPercentage) { this.completionPercentage = completionPercentage; }
        public int getDaysRemaining() { return daysRemaining; }
        public void setDaysRemaining(int daysRemaining) { this.daysRemaining = daysRemaining; }
    }

    public static class ProgressSummary {
        private long totalSessions;
        private long completedGoals;
        private long activeGoals;
        private long totalPoints;
        private int currentStreak;
        private int weeklySessions;
        private int weeklyMinutes;

        // Getters and setters
        public long getTotalSessions() { return totalSessions; }
        public void setTotalSessions(long totalSessions) { this.totalSessions = totalSessions; }
        public long getCompletedGoals() { return completedGoals; }
        public void setCompletedGoals(long completedGoals) { this.completedGoals = completedGoals; }
        public long getActiveGoals() { return activeGoals; }
        public void setActiveGoals(long activeGoals) { this.activeGoals = activeGoals; }
        public long getTotalPoints() { return totalPoints; }
        public void setTotalPoints(long totalPoints) { this.totalPoints = totalPoints; }
        public int getCurrentStreak() { return currentStreak; }
        public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }
        public int getWeeklySessions() { return weeklySessions; }
        public void setWeeklySessions(int weeklySessions) { this.weeklySessions = weeklySessions; }
        public int getWeeklyMinutes() { return weeklyMinutes; }
        public void setWeeklyMinutes(int weeklyMinutes) { this.weeklyMinutes = weeklyMinutes; }
    }
}
