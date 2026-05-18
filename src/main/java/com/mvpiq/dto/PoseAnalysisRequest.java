package com.mvpiq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoseAnalysisRequest {
    
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
}
