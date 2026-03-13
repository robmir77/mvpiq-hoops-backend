package com.mvpiq.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShotMetrics {

    // -------------------------
    // SHOOTING METRICS
    // -------------------------

    private double releaseAngle;
    private double entryAngle;
    private double releaseSpeed;
    private double arcHeight;
    private double trajectoryQuality;

    // -------------------------
    // SHOT RESULT
    // -------------------------

    private boolean make;
    private String missType;

}