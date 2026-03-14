package com.mvpiq.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ShotMetrics {

    // -------------------------
    // BALL TRAJECTORY
    // -------------------------

    private double releaseAngle;
    private double entryAngle;
    private double releaseSpeed;
    private double arcHeight;
    private double trajectoryQuality;

    // -------------------------
    // RELEASE METRICS
    // -------------------------

    private double releaseHeight;
    private int releaseFrame;

    // -------------------------
    // ARM BIOMECHANICS
    // -------------------------

    private double elbowAngle;
    private double wristAngle;
    private double elbowAlignmentError;

    // -------------------------
    // BODY BALANCE
    // -------------------------

    private double bodyBalanceScore;

    // -------------------------
    // SCORING
    // -------------------------

    private double overallScore;

    // -------------------------
    // FEEDBACK
    // -------------------------

    private List<String> suggestions;

    // -------------------------
    // SHOT RESULT
    // -------------------------

    private boolean make;
    private String missType;

}