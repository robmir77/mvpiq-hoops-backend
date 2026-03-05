package com.mvpiq.dto;

import com.mvpiq.model.Ranking;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RankingDTO {

    private UUID id;
    private UUID playerId;
    private String rankScope;
    private String scopeValue;
    private Integer rankPosition;
    private BigDecimal score;
    private Integer seasonYear;

    // --- Conversione da Entity a DTO ---
    public static RankingDTO fromEntity(Ranking ranking) {
        if (ranking == null) return null;
        return RankingDTO.builder()
                .id(ranking.getId())
                .playerId(ranking.getPlayerId())
                .rankScope(ranking.getRankScope())
                .scopeValue(ranking.getScopeValue())
                .rankPosition(ranking.getRankPosition())
                .score(ranking.getScore())
                .seasonYear(ranking.getSeasonYear())
                .build();
    }

    // --- Conversione da DTO a Entity ---
    public static Ranking toEntity(RankingDTO dto) {
        if (dto == null) return null;
        Ranking ranking = new Ranking();
        ranking.setId(dto.getId());
        ranking.setPlayerId(dto.getPlayerId());
        ranking.setRankScope(dto.getRankScope());
        ranking.setScopeValue(dto.getScopeValue());
        ranking.setRankPosition(dto.getRankPosition());
        ranking.setScore(dto.getScore());
        ranking.setSeasonYear(dto.getSeasonYear());
        return ranking;
    }
}
