package com.mvpiq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShotChartResponse {
    
    private List<ShotPoint> shots;
    private SessionStats sessionStats;
    private ZoneStats zoneStats;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShotPoint {
        private Double x;
        private Double y;
        private Boolean made;
        private Double distance;
        private String zone;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionStats {
        private Integer totalShots;
        private Integer madeShots;
        private Integer missedShots;
        private Double shootingPercentage;
        private Double averageDistance;
        private String bestZone;
        private String worstZone;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ZoneStats {
        private ZonePaint paint;
        private ZoneMidRange midRange;
        private ZoneThreePoint threePoint;
        private ZoneCorner corner;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ZonePaint {
        private Integer attempts;
        private Integer made;
        private Double percentage;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ZoneMidRange {
        private Integer attempts;
        private Integer made;
        private Double percentage;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ZoneThreePoint {
        private Integer attempts;
        private Integer made;
        private Double percentage;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ZoneCorner {
        private Integer attempts;
        private Integer made;
        private Double percentage;
    }
}
