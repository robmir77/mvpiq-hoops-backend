package com.mvpiq.service;

import com.mvpiq.model.*;
import com.mvpiq.repositories.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
public class RankingCalculationService {

    @Inject
    RankingRepository rankingRepository;
    
    @Inject
    AthletePointsRepository athletePointsRepository;
    
    @Inject
    UserRepository userRepository;
    
    @Inject
    PlayerProfileRepository playerProfileRepository;

    /**
     * Ricalcola tutti i ranking globali
     */
    @Transactional
    public void recalculateAllRankings() {
        log.info("Starting global ranking recalculation");
        
        int currentYear = Year.now().getValue();
        
        // Calcola ranking per ogni scope
        recalculateRankingByScope("GLOBAL", null, currentYear);
        recalculateRankingByScope("COUNTRY", null, currentYear);
        recalculateRankingByScope("AGE", null, currentYear);
        recalculateRankingByScope("ROLE", null, currentYear);
        
        log.info("Global ranking recalculation completed");
    }

    /**
     * Ricalcola ranking per uno scope specifico
     */
    @Transactional
    public void recalculateRankingByScope(String rankScope, String scopeValue, int seasonYear) {
        log.info("Recalculating rankings for scope: {}, value: {}, year: {}", rankScope, scopeValue, seasonYear);
        
        // Recupera tutti i giocatori attivi
        List<User> players = userRepository.findByRole("player");
        
        // Calcola punteggi e crea ranking entries
        List<Ranking> rankings = new ArrayList<>();
        
        for (User player : players) {
            int score = calculatePlayerScore(player.getId(), rankScope, scopeValue);
            
            Ranking ranking = new Ranking();
            ranking.setPlayer((PlayerProfile) player);
            ranking.setScope(rankScope);
            ranking.setScopeValue(scopeValue);
            ranking.setScore(java.math.BigDecimal.valueOf(score));
            ranking.setSeasonYear(seasonYear);
            ranking.setCreatedAt(OffsetDateTime.now());
            
            rankings.add(ranking);
        }
        
        // Ordina per punteggio decrescente
        rankings.sort((a, b) -> b.getScore().compareTo(a.getScore()));
        
        // Assegna posizioni
        for (int i = 0; i < rankings.size(); i++) {
            rankings.get(i).setRankPosition(i + 1);
        }
        
        // Rimuovi vecchi ranking per questo scope
        rankingRepository.deleteByRankScopeAndScopeValueAndSeasonYear(rankScope, scopeValue, seasonYear);
        
        // Salva nuovi ranking
        rankingRepository.persist(rankings);
        
        log.info("Saved {} rankings for scope: {}", rankings.size(), rankScope);
    }

    /**
     * Aggiorna i punti di un singolo giocatore
     */
    @Transactional
    public void updatePlayerPoints(UUID userId) {
        log.info("Updating points for player: {}", userId);
        
        int totalScore = calculatePlayerScore(userId, "GLOBAL", null);
        
        Optional<AthletePoints> existingPoints = athletePointsRepository.findByIdOptional(userId);
        if (existingPoints.isPresent()) {
            AthletePoints points = existingPoints.get();
            points.setTotalPoints((long) totalScore);
            points.setUpdatedAt(OffsetDateTime.now());
        } else {
            AthletePoints points = new AthletePoints();
            Optional<PlayerProfile> playerOpt = playerProfileRepository.findByIdOptional(userId);
            if (playerOpt.isPresent()) {
                points.setPlayer(playerOpt.get());
            }
            points.setTotalPoints((long) totalScore);
            points.setUpdatedAt(OffsetDateTime.now());
            athletePointsRepository.persist(points);
        }
        
        // Aggiorna i ranking che coinvolgono questo giocatore
        updatePlayerRankings(userId);
        
        log.info("Updated points for player {}: {}", userId, totalScore);
    }

    /**
     * Genera leaderboard per uno scope specifico
     */
    public List<Ranking> getLeaderboard(String rankScope, String scopeValue, int limit) {
        int currentYear = Year.now().getValue();
        
        return rankingRepository.findByRankScopeAndScopeValueAndSeasonYearOrderByRankPositionAsc(
                rankScope, scopeValue, currentYear)
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Ottiene ranking di un giocatore per tutti gli scope
     */
    public List<Ranking> getPlayerRankings(UUID playerId) {
        int currentYear = Year.now().getValue();
        
        return rankingRepository.findByPlayerIdAndSeasonYearOrderByRankScope(playerId, currentYear);
    }

    /**
     * Calcola il punteggio di un giocatore basato su vari fattori
     */
    private int calculatePlayerScore(UUID playerId, String rankScope, String scopeValue) {
        int score = 0;
        
        // Punti base da badge e achievement
        Optional<AthletePoints> athletePoints = athletePointsRepository.findByIdOptional(playerId);
        if (athletePoints.isPresent()) {
            score += athletePoints.get().getTotalPoints();
        }
        
        // Punti da sessioni di training
        List<TrainingSession> sessions = trainingSessionRepository.findByUserId(playerId);
        score += sessions.size() * 5; // 5 punti per sessione
        
        // Punti da video analysis
        List<VideoAnalysisSession> videoSessions = videoAnalysisSessionRepository.findByUserId(playerId);
        score += videoSessions.size() * 10; // 10 punti per analisi video
        
        // Punti bonus per specializzazioni (scope-specific)
        if ("ROLE".equals(rankScope) && scopeValue != null) {
            score += calculateRoleBonus(playerId, scopeValue);
        }
        
        if ("AGE".equals(rankScope) && scopeValue != null) {
            score += calculateAgeBonus(playerId, scopeValue);
        }
        
        if ("COUNTRY".equals(rankScope) && scopeValue != null) {
            score += calculateCountryBonus(playerId, scopeValue);
        }
        
        return score;
    }

    /**
     * Aggiorna i ranking di un giocatore per tutti gli scope
     */
    private void updatePlayerRankings(UUID playerId) {
        int currentYear = Year.now().getValue();
        
        // Aggiorna ranking globali
        recalculateRankingByScope("GLOBAL", null, currentYear);
        
        // Aggiorna ranking per paese
        PlayerProfile player = playerProfileRepository.findById(playerId);
        if (player != null) {
            if (player.getCountry() != null) {
                recalculateRankingByScope("COUNTRY", player.getCountry(), currentYear);
            }
            
            // Aggiorna ranking per età
            if (player.getApproximateAge() != null) {
                String ageGroup = getAgeGroup(player.getApproximateAge());
                recalculateRankingByScope("AGE", ageGroup, currentYear);
            }
        }
        
        // Aggiorna ranking per ruolo
        recalculateRankingByScope("ROLE", null, currentYear);
    }

    /**
     * Calcola bonus per ruolo specifico
     */
    private int calculateRoleBonus(UUID playerId, String role) {
        // Implementazione semplificata - bonus basati su esercizi specifici del ruolo
        return 50; // Bonus fisso per ora
    }

    /**
     * Calcola bonus per fascia d'età
     */
    private int calculateAgeBonus(UUID playerId, String ageGroup) {
        // Bonus basati su performance relativa all'età
        switch (ageGroup) {
            case "U16": return 30;
            case "U18": return 40;
            case "U20": return 50;
            case "U23": return 60;
            case "SENIOR": return 100;
            default: return 0;
        }
    }

    /**
     * Calcola bonus per paese
     */
    private int calculateCountryBonus(UUID playerId, String country) {
        // Bonus basati su competitività del paese
        return 25; // Bonus fisso per ora
    }

    /**
     * Converte età in fascia d'età
     */
    private String getAgeGroup(Integer age) {
        if (age == null) return "UNKNOWN";
        
        if (age <= 15) return "U16";
        if (age <= 17) return "U18";
        if (age <= 19) return "U20";
        if (age <= 22) return "U23";
        return "SENIOR";
    }

    // Repository injection (da aggiungere se mancanti)
    @Inject
    TrainingSessionRepository trainingSessionRepository;
    
    @Inject
    VideoAnalysisSessionRepository videoAnalysisSessionRepository;
}
