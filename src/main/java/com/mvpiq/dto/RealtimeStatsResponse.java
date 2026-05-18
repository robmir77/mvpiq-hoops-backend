package com.mvpiq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeStatsResponse {
    
    private UUID sessionId;
    private Integer shotCount;
    private BigDecimal fieldGoalPercentage;
    private Integer shotStreak;
    private BigDecimal releaseAngleAvg;
    private BigDecimal releaseVelocityAvg;
    private Map<String, Integer> heatZones;
    private List<ShotPosition> recentShots;
    private Long sessionDuration;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShotPosition {
        private Double courtX;
        private Double courtY;
        private String result;
        private Long timestamp;
    }
}
