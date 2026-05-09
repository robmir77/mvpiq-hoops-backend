package com.mvpiq.service;

import com.mvpiq.model.PlayerProfile;
import com.mvpiq.model.User;
import com.mvpiq.repositories.PlayerProfileRepository;
import com.mvpiq.repositories.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.Period;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@ApplicationScoped
public class PlayerProfileService {

    @Inject
    PlayerProfileRepository playerProfileRepository;

    @Inject
    UserRepository userRepository;

    @Transactional
    public PlayerProfile createPlayerProfile(PlayerProfile profile, UUID userId) {
        log.info("Creating player profile for user: {}", userId);
        
        Optional<User> user = Optional.ofNullable(userRepository.findById(userId));
        if (user.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
        
        // Calculate approximate age if birth date is provided
        if (profile.getBirthDate() != null) {
            int age = Period.between(profile.getBirthDate(), LocalDate.now()).getYears();
            profile.setApproximateAge(age);
        }
        
        profile.setId(userId); // Use same ID as user
        profile.setUpdatedAt(OffsetDateTime.now());
        
        playerProfileRepository.persist(profile);
        log.info("Player profile created with ID: {}", profile.getId());
        return profile;
    }

    @Transactional
    public PlayerProfile updatePlayerProfile(UUID profileId, PlayerProfile updatedProfile) {
        log.info("Updating player profile: {}", profileId);
        
        Optional<PlayerProfile> existingProfile = Optional.ofNullable(playerProfileRepository.findById(profileId));
        if (existingProfile.isEmpty()) {
            throw new IllegalArgumentException("Player profile not found: " + profileId);
        }
        
        PlayerProfile profile = existingProfile.get();
        profile.setFullName(updatedProfile.getFullName());
        profile.setBirthDate(updatedProfile.getBirthDate());
        profile.setHeightCm(updatedProfile.getHeightCm());
        profile.setWeightKg(updatedProfile.getWeightKg());
        profile.setMainPosition(updatedProfile.getMainPosition());
        profile.setSecondaryPositions(updatedProfile.getSecondaryPositions());
        profile.setLevel(updatedProfile.getLevel());
        profile.setDominantHand(updatedProfile.getDominantHand());
        profile.setCountry(updatedProfile.getCountry());
        profile.setCity(updatedProfile.getCity());
        profile.setGender(updatedProfile.getGender());
        profile.setUpdatedAt(OffsetDateTime.now());
        
        // Recalculate age if birth date changed
        if (updatedProfile.getBirthDate() != null) {
            int age = Period.between(updatedProfile.getBirthDate(), LocalDate.now()).getYears();
            profile.setApproximateAge(age);
        }
        
        playerProfileRepository.persist(profile);
        log.info("Player profile updated: {}", profileId);
        return profile;
    }

    public Optional<PlayerProfile> getPlayerProfile(UUID profileId) {
        return Optional.ofNullable(playerProfileRepository.findById(profileId));
    }

    public Optional<PlayerProfile> getPlayerProfileByUserId(UUID userId) {
        return Optional.ofNullable(playerProfileRepository.findByUserId(userId));
    }

    public List<PlayerProfile> getPlayersByCountry(String country) {
        return playerProfileRepository.findByCountry(country);
    }

    public List<PlayerProfile> getPlayersByLevel(String level) {
        return playerProfileRepository.findByLevel(level);
    }

    public List<PlayerProfile> getPlayersByPosition(String position) {
        return playerProfileRepository.findByMainPosition(position);
    }

    public List<PlayerProfile> getPlayersByAgeRange(int minAge, int maxAge) {
        return playerProfileRepository.findByAgeRange(minAge, maxAge);
    }

    public List<PlayerProfile> getPublicPlayerProfiles() {
        return playerProfileRepository.findByPublicProfile(true);
    }

    @Transactional
    public void deletePlayerProfile(UUID profileId) {
        log.info("Deleting player profile: {}", profileId);
        
        PlayerProfile profile = playerProfileRepository.findById(profileId);
        if (profile != null) {
            playerProfileRepository.delete(profile);
            log.info("Player profile deleted: {}", profileId);
        }
    }

    /**
     * Get player statistics for profile
     */
    public Map<String, Object> getPlayerStats(UUID profileId) {
        log.info("Getting stats for player: {}", profileId);
        
        Optional<PlayerProfile> profile = Optional.ofNullable(playerProfileRepository.findById(profileId));
        if (profile.isEmpty()) {
            throw new IllegalArgumentException("Player profile not found: " + profileId);
        }
        
        // Collect player statistics from various sources
        // This would include CV stats, training sessions, achievements, etc.
        return Map.of(
            "profileId", profileId,
            "totalSessions", 0,
            "totalGoals", 0,
            "totalBadges", 0,
            "totalPoints", 0
        );
    }

    /**
     * Search players with advanced filters
     */
    public List<PlayerProfile> searchPlayers(Map<String, Object> filters) {
        log.info("Searching players with filters: {}", filters);
        
        // Implement advanced search logic
        // This would use the repository methods or custom queries
        return List.of(); // Placeholder - implement actual search
    }

    /**
     * Get player rankings across different scopes
     */
    public List<Map<String, Object>> getPlayerRankings(UUID profileId) {
        log.info("Getting rankings for player: {}", profileId);
        
        // Implement ranking retrieval from ranking service
        return List.of(); // Placeholder - implement actual ranking logic
    }
}
