package com.mvpiq.service;

import com.mvpiq.dto.PlayerCvDTO;
import com.mvpiq.dto.PlayerCvTeamDTO;
import com.mvpiq.model.*;
import com.mvpiq.repositories.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import java.time.OffsetDateTime;
import java.time.Year;
import java.util.*;

@ApplicationScoped
@Transactional
public class PlayerCvService {

    @Inject
    PlayerRepository playerRepository;

    @Inject
    PlayerCvRepository cvRepository;

    @Inject
    PlayerCvTeamRepository teamRepository;

    @Inject
    PlayerCvHighlightRepository highlightRepository;

    @Inject
    MediaAssetRepository mediaAssetRepository;

    @Inject
    PositionMetadataRepository positionRepository;

    // ===============================
    // GET CV
    // ===============================
    public PlayerCvDTO getCv(UUID playerId) {

        // Try to find existing CV, create empty one if not found
        PlayerCv cv = cvRepository.findByPlayerIdNativeQuery(playerId)
                .orElseGet(() -> {
                    Player player = playerRepository.findByIdOptional(playerId)
                            .orElseThrow(() -> new NotFoundException("Player not found"));
                    PlayerCv newCv = new PlayerCv();
                    newCv.setPlayer(player);
                    cvRepository.persist(newCv);
                    return newCv;
                });

        List<PlayerCvTeam> teams = teamRepository.findByPlayerIdColumn(playerId);

        return toDTO(cv, teams);
    }

    // ===============================
    // UPDATE CV
    // ===============================
    @Transactional
    public PlayerCvDTO updateCv(UUID playerId, PlayerCvDTO dto) {

        Player player = playerRepository.findByIdOptional(playerId)
                .orElseThrow(() -> new NotFoundException("Player not found"));

        // 1️⃣ Load or create CV
        PlayerCv cv = cvRepository.findByPlayerIdNativeQuery(playerId)
                .orElseGet(() -> {
                    PlayerCv newCv = new PlayerCv();
                    newCv.setPlayer(player);
                    cvRepository.persist(newCv);
                    return newCv;
                });

        // 2️⃣ Update basic fields
        cv.setHeadline(dto.getHeadline());
        cv.setSummary(dto.getSummary());
        cv.setStats(dto.getStats());

        // 3️⃣ Delete old teams by CV (CORRETTO)
        teamRepository.deleteByCvId(cv.getId());

        // 4️⃣ Rebuild teams
        if (dto.getTeams() != null && !dto.getTeams().isEmpty()) {

            for (PlayerCvTeamDTO teamDto : dto.getTeams()) {

                validateTeamYears(teamDto);

                PlayerCvTeam team = new PlayerCvTeam();
                team.setCv(cv); // 🔥 CORRETTO
                team.setTeamName(teamDto.getTeamName());
                team.setCategoryId(teamDto.getCategoryId());
                team.setStartYear(teamDto.getStartYear());
                team.setEndYear(teamDto.getEndYear());
                team.setNotes(teamDto.getNotes());

                if (teamDto.getPositionId() != null) {

                    PositionMetadata position = positionRepository
                            .find("id = ?1 and isActive = true", teamDto.getPositionId())
                            .firstResultOptional()
                            .orElseThrow(() -> new NotFoundException("Active position not found"));

                    team.setPosition(position);
                }

                teamRepository.persist(team);
            }
        }

        return getCv(playerId);
    }

    private void validateTeamYears(PlayerCvTeamDTO dto) {

        Integer start = dto.getStartYear();
        Integer end = dto.getEndYear();
        int currentYear = Year.now().getValue();

        if (start == null) {
            throw new BadRequestException("Start year is required");
        }

        if (start < 1000 || start > currentYear) {
            throw new BadRequestException("Start year is not valid");
        }

        if (end != null) {
            if (end < start) {
                throw new BadRequestException("End year cannot be before start year");
            }

            if (end > currentYear) {
                throw new BadRequestException("End year cannot be in the future");
            }
        }
    }

    // ===============================
    // MAPPER
    // ===============================
    private PlayerCvDTO toDTO(PlayerCv cv, List<PlayerCvTeam> teams) {

        PlayerCvDTO dto = new PlayerCvDTO();
        dto.setHeadline(cv.getHeadline());
        dto.setSummary(cv.getSummary());
        dto.setStats(cv.getStats());

        List<PlayerCvTeamDTO> teamDTOs = teams.stream().map(t -> {
            PlayerCvTeamDTO teamDto = new PlayerCvTeamDTO();
            teamDto.setId(t.getId());
            teamDto.setTeamName(t.getTeamName());
            teamDto.setCategoryId(t.getCategoryId());
            teamDto.setStartYear(t.getStartYear());
            teamDto.setEndYear(t.getEndYear());
            teamDto.setNotes(t.getNotes());

            if (t.getPosition() != null) {
                teamDto.setPositionId(t.getPosition().getId());
            }

            return teamDto;
        }).toList();

        dto.setTeams(teamDTOs);

        return dto;
    }

    // ===============================
    // SHARE CV
    // ===============================
    public Map<String, Object> enableSharing(UUID playerId) {
        PlayerCv cv = cvRepository.findByPlayerIdNativeQuery(playerId)
                .orElseThrow(() -> new NotFoundException("CV not found"));

        // Generate new share token if not exists
        if (cv.getShareToken() == null) {
            cv.setShareToken(UUID.randomUUID());
        }

        cv.setShareEnabled(true);
        cv.setPublicUpdatedAt(OffsetDateTime.now());
        cvRepository.persist(cv);

        String publicUrl = "https://app.mvpiq-hoops.com/public/cv/" + cv.getShareToken();

        Map<String, Object> response = new HashMap<>();
        response.put("shareToken", cv.getShareToken());
        response.put("publicUrl", publicUrl);
        response.put("shareEnabled", true);
        response.put("publicUpdatedAt", cv.getPublicUpdatedAt());

        return response;
    }

    public Map<String, Object> disableSharing(UUID playerId) {
        PlayerCv cv = cvRepository.findByPlayerIdNativeQuery(playerId)
                .orElseThrow(() -> new NotFoundException("CV not found"));

        cv.setShareEnabled(false);
        cv.setPublicUpdatedAt(OffsetDateTime.now());
        cvRepository.persist(cv);

        Map<String, Object> response = new HashMap<>();
        response.put("shareEnabled", false);
        response.put("publicUpdatedAt", cv.getPublicUpdatedAt());

        return response;
    }

    public PlayerCvDTO getPublicCv(UUID shareToken) {
        PlayerCv cv = cvRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new NotFoundException("CV not found or not shared"));

        if (!cv.getShareEnabled()) {
            throw new NotFoundException("CV is not publicly shared");
        }

        List<PlayerCvTeam> teams = teamRepository.findByCvId(cv.getId());
        return toDTO(cv, teams);
    }

    // ===============================
    // HIGHLIGHTS
    // ===============================
    public PlayerCvHighlight addHighlight(UUID playerId, UUID mediaId, String title, String description) {
        PlayerCv cv = cvRepository.findByPlayerIdNativeQuery(playerId)
                .orElseThrow(() -> new NotFoundException("CV not found"));

        MediaAsset media = mediaAssetRepository.findByIdOptional(mediaId)
                .orElseThrow(() -> new NotFoundException("Media asset not found"));

        // Get current max sort order
        Integer maxSortOrder = highlightRepository.findMaxSortOrderByCvId(cv.getId());
        Integer newSortOrder = maxSortOrder != null ? maxSortOrder + 1 : 0;

        PlayerCvHighlight highlight = PlayerCvHighlight.builder()
                .cv(cv)
                .media(media)
                .title(title)
                .description(description)
                .sortOrder(newSortOrder)
                .build();

        highlightRepository.persist(highlight);
        return highlight;
    }

    public PlayerCvHighlight addExternalHighlight(UUID playerId, String externalUrl, String title, String description) {
        PlayerCv cv = cvRepository.findByPlayerIdNativeQuery(playerId)
                .orElseThrow(() -> new NotFoundException("CV not found"));

        // Validate URL format
        if (externalUrl == null || externalUrl.trim().isEmpty()) {
            throw new BadRequestException("External URL is required");
        }

        // Get current max sort order
        Integer maxSortOrder = highlightRepository.findMaxSortOrderByCvId(cv.getId());
        Integer newSortOrder = maxSortOrder != null ? maxSortOrder + 1 : 0;

        PlayerCvHighlight highlight = PlayerCvHighlight.builder()
                .cv(cv)
                .externalUrl(externalUrl)
                .title(title)
                .description(description)
                .sortOrder(newSortOrder)
                .build();

        highlightRepository.persist(highlight);
        return highlight;
    }

    public void deleteHighlight(UUID playerId, UUID highlightId) {
        PlayerCv cv = cvRepository.findByPlayerIdNativeQuery(playerId)
                .orElseThrow(() -> new NotFoundException("CV not found"));

        PlayerCvHighlight highlight = highlightRepository.findByIdOptional(highlightId)
                .orElseThrow(() -> new NotFoundException("Highlight not found"));

        // Verify ownership
        if (!highlight.getCv().getId().equals(cv.getId())) {
            throw new BadRequestException("Highlight does not belong to this CV");
        }

        highlightRepository.delete(highlight);
    }

    public List<PlayerCvHighlight> getHighlights(UUID playerId) {
        PlayerCv cv = cvRepository.findByPlayerIdNativeQuery(playerId)
                .orElseThrow(() -> new NotFoundException("CV not found"));

        return highlightRepository.findByCvIdOrderBySortOrder(cv.getId());
    }
}