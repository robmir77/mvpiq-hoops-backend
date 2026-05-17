package com.mvpiq.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CareerStatsDTO {
    private int totalSessions;
    private int totalShots;
    private int totalMade;
    private int totalMissed;
    private double overallPercentage;
    private double bestSessionPercentage;
    private String favoriteZone;
}
