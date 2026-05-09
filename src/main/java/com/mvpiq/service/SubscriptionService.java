package com.mvpiq.service;

import com.mvpiq.model.User;
import com.mvpiq.repositories.UserRepository;
import com.mvpiq.enums.UserRole;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@ApplicationScoped
public class SubscriptionService {

    @Inject
    UserRepository userRepository;

    /**
     * Verifica se un utente ha accesso Premium
     */
    public boolean isPremium(UUID userId) {
        User user = userRepository.findById(userId);
        if (user == null) {
            return false;
        }
        
        // I trainer e scout hanno accesso premium di default
        if (UserRole.trainer.equals(user.getRole()) || UserRole.scout.equals(user.getRole())) {
            return true;
        }
        
        // Per i player, implementazione semplificata - tutti premium per ora
        // In produzione, qui ci sarebbe logica di subscription/pagamento
        return UserRole.player.equals(user.getRole());
    }

    /**
     * Verifica se un utente ha accesso da Scout
     */
    public boolean hasScoutAccess(UUID userId) {
        User user = userRepository.findById(userId);
        if (user == null) {
            return false;
        }
        return UserRole.scout.equals(user.getRole()) || UserRole.admin.equals(user.getRole());
    }

    /**
     * Verifica se un utente ha accesso da Creator
     */
    public boolean hasCreatorAccess(UUID userId) {
        User user = userRepository.findById(userId);
        if (user == null) {
            return false;
        }
        return UserRole.creator.equals(user.getRole()) || UserRole.trainer.equals(user.getRole()) || UserRole.admin.equals(user.getRole());
    }

    /**
     * Verifica se un utente può accedere a funzionalità avanzate
     */
    public boolean hasAdvancedFeatures(UUID userId) {
        return isPremium(userId) || hasScoutAccess(userId) || hasCreatorAccess(userId);
    }

    /**
     * Ottiene il piano di subscription di un utente
     */
    public SubscriptionPlan getUserPlan(UUID userId) {
        User user = userRepository.findById(userId);
        if (user == null) {
            return SubscriptionPlan.FREE;
        }
        
        switch (user.getRole()) {
            case admin:
                return SubscriptionPlan.ADMIN;
            case scout:
                return SubscriptionPlan.SCOUT_PRO;
            case trainer:
            case creator:
                return SubscriptionPlan.PREMIUM;
            case player:
                return isPremium(userId) ? SubscriptionPlan.PREMIUM : SubscriptionPlan.FREE;
            default:
                return SubscriptionPlan.FREE;
        }
    }

    /**
     * Aggiorna un utente a Premium (implementazione semplificata)
     */
    @Transactional
    public boolean upgradeToPremium(UUID userId) {
        User user = userRepository.findById(userId);
        if (user == null) {
            log.warn("User not found for premium upgrade: {}", userId);
            return false;
        }
        
        // In produzione, qui ci sarebbe logica di pagamento
        // Per ora, tutti i player possono diventare premium
        
        log.info("Upgraded user {} to premium plan", userId);
        return true;
    }

    /**
     * Verifica se un utente può creare contenuti ufficiali
     */
    public boolean canCreateOfficialContent(UUID userId) {
        return hasCreatorAccess(userId);
    }

    /**
     * Verifica se un utente può accedere a analytics avanzati
     */
    public boolean canAccessAdvancedAnalytics(UUID userId) {
        return hasScoutAccess(userId) || hasCreatorAccess(userId);
    }

    /**
     * Verifica se un utente può usare filtri avanzati di ricerca
     */
    public boolean canUseAdvancedFilters(UUID userId) {
        return hasScoutAccess(userId) || isPremium(userId);
    }

    /**
     * Verifica limiti di upload video per un utente
     */
    public VideoUploadLimits getVideoUploadLimits(UUID userId) {
        SubscriptionPlan plan = getUserPlan(userId);
        
        switch (plan) {
            case FREE:
                return new VideoUploadLimits(5, 100, 50); // 5 video/mese, 100MB max, 50 min total
            case PREMIUM:
                return new VideoUploadLimits(50, 500, 300); // 50 video/mese, 500MB max, 5 ore total
            case SCOUT_PRO:
                return new VideoUploadLimits(100, 1000, 600); // 100 video/mese, 1GB max, 10 ore total
            case ADMIN:
                return new VideoUploadLimits(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE); // Illimitati
            default:
                return new VideoUploadLimits(5, 100, 50);
        }
    }

    /**
     * Verifica limiti di analisi video per un utente
     */
    public VideoAnalysisLimits getVideoAnalysisLimits(UUID userId) {
        SubscriptionPlan plan = getUserPlan(userId);
        
        switch (plan) {
            case FREE:
                return new VideoAnalysisLimits(10, 5); // 10 analisi/mese, max 5 minuti
            case PREMIUM:
                return new VideoAnalysisLimits(100, 10); // 100 analisi/mese, max 10 minuti
            case SCOUT_PRO:
                return new VideoAnalysisLimits(500, 15); // 500 analisi/mese, max 15 minuti
            case ADMIN:
                return new VideoAnalysisLimits(Integer.MAX_VALUE, 30); // Illimitate, max 30 minuti
            default:
                return new VideoAnalysisLimits(10, 5);
        }
    }

    /**
     * Verifica se un utente può accedere a un determinato feature
     */
    public boolean canAccessFeature(UUID userId, String feature) {
        SubscriptionPlan plan = getUserPlan(userId);
        
        switch (feature.toLowerCase()) {
            case "video_upload":
                return plan != SubscriptionPlan.FREE;
            case "video_analysis":
                return plan != SubscriptionPlan.FREE;
            case "advanced_analytics":
                return plan == SubscriptionPlan.SCOUT_PRO || plan == SubscriptionPlan.ADMIN;
            case "content_creation":
                return hasCreatorAccess(userId);
            case "scouting_tools":
                return hasScoutAccess(userId);
            case "premium_programs":
                return plan == SubscriptionPlan.PREMIUM || plan == SubscriptionPlan.SCOUT_PRO || plan == SubscriptionPlan.ADMIN;
            case "unlimited_storage":
                return plan == SubscriptionPlan.SCOUT_PRO || plan == SubscriptionPlan.ADMIN;
            default:
                return false;
        }
    }

    /**
     * Ottiene i dettagli della subscription di un utente
     */
    public SubscriptionDetails getSubscriptionDetails(UUID userId) {
        SubscriptionPlan plan = getUserPlan(userId);
        SubscriptionDetails details = new SubscriptionDetails();
        
        details.setUserId(userId);
        details.setPlan(plan);
        details.setStartDate(OffsetDateTime.now().minusDays(30)); // Simulato
        details.setEndDate(OffsetDateTime.now().plusDays(30)); // Simulato
        details.setAutoRenewal(plan != SubscriptionPlan.FREE);
        details.setActive(true);
        
        return details;
    }

    // Enum e DTO classes
    public enum SubscriptionPlan {
        FREE,
        PREMIUM,
        SCOUT_PRO,
        ADMIN
    }

    public static class VideoUploadLimits {
        private final int maxVideosPerMonth;
        private final int maxFileSizeMB;
        private final int maxTotalMinutesPerMonth;

        public VideoUploadLimits(int maxVideosPerMonth, int maxFileSizeMB, int maxTotalMinutesPerMonth) {
            this.maxVideosPerMonth = maxVideosPerMonth;
            this.maxFileSizeMB = maxFileSizeMB;
            this.maxTotalMinutesPerMonth = maxTotalMinutesPerMonth;
        }

        public int getMaxVideosPerMonth() { return maxVideosPerMonth; }
        public int getMaxFileSizeMB() { return maxFileSizeMB; }
        public int getMaxTotalMinutesPerMonth() { return maxTotalMinutesPerMonth; }
    }

    public static class VideoAnalysisLimits {
        private final int maxAnalysesPerMonth;
        private final int maxVideoLengthMinutes;

        public VideoAnalysisLimits(int maxAnalysesPerMonth, int maxVideoLengthMinutes) {
            this.maxAnalysesPerMonth = maxAnalysesPerMonth;
            this.maxVideoLengthMinutes = maxVideoLengthMinutes;
        }

        public int getMaxAnalysesPerMonth() { return maxAnalysesPerMonth; }
        public int getMaxVideoLengthMinutes() { return maxVideoLengthMinutes; }
    }

    public static class SubscriptionDetails {
        private UUID userId;
        private SubscriptionPlan plan;
        private OffsetDateTime startDate;
        private OffsetDateTime endDate;
        private boolean autoRenewal;
        private boolean active;

        // Getters and setters
        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }
        public SubscriptionPlan getPlan() { return plan; }
        public void setPlan(SubscriptionPlan plan) { this.plan = plan; }
        public OffsetDateTime getStartDate() { return startDate; }
        public void setStartDate(OffsetDateTime startDate) { this.startDate = startDate; }
        public OffsetDateTime getEndDate() { return endDate; }
        public void setEndDate(OffsetDateTime endDate) { this.endDate = endDate; }
        public boolean isAutoRenewal() { return autoRenewal; }
        public void setAutoRenewal(boolean autoRenewal) { this.autoRenewal = autoRenewal; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }
}
