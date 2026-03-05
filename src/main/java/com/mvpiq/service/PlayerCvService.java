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
    PositionMetadataRepository positionRepository;

    // ===============================
    // GET CV
    // ===============================
    public PlayerCvDTO getCv(UUID playerId) {

        PlayerCv cv = cvRepository.findByPlayer(playerId)
                .orElseThrow(() -> new NotFoundException("CV not found"));

        List<PlayerCvTeam> teams = teamRepository.findByPlayer(playerId);

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
        PlayerCv cv = cvRepository.findByPlayer(playerId)
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
        teamRepository.delete("cv.id", cv.getId());

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

        if (start < 1900 || start > currentYear) {
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
}