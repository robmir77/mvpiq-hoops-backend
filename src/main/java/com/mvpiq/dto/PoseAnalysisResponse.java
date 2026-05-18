package com.mvpiq.dto;

import com.mvpiq.model.PoseAnalysis;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class PoseAnalysisResponse {
    private UUID id;
    private UUID shotEventId;
    private BigDecimal elbowAngle;
    private BigDecimal kneeAngle;
    private BigDecimal shoulderAngle;
    private BigDecimal wristAngle;
    private BigDecimal releaseHeight;
    private BigDecimal releaseAngle;
    private BigDecimal releaseVelocity;
    private BigDecimal shotSmoothness;
    private BigDecimal followThroughScore;
    private BigDecimal balanceScore;
    private OffsetDateTime createdAt;

    public static PoseAnalysisResponse from(PoseAnalysis p) {
        return PoseAnalysisResponse.builder()
                .id(p.getId())
                .shotEventId(p.getShotEvent() != null ? p.getShotEvent().getId() : null)
                .elbowAngle(p.getElbowAngle())
                .kneeAngle(p.getKneeAngle())
                .shoulderAngle(p.getShoulderAngle())
                .wristAngle(p.getWristAngle())
                .releaseHeight(p.getReleaseHeight())
                .releaseAngle(p.getReleaseAngle())
                .releaseVelocity(p.getReleaseVelocity())
                .shotSmoothness(p.getShotSmoothness())
                .followThroughScore(p.getFollowThroughScore())
                .balanceScore(p.getBalanceScore())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
