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
    private double releaseAngle;      // angolo di rilascio (°)
    private double entryAngle;        // angolo d’ingresso (°)
    private double releaseSpeed;      // velocità al rilascio (cm/frame)
    private double arcHeight;         // altezza massima traiettoria (cm)
    private double trajectoryQuality; // qualità della traiettoria (0-1)
    private double distance;          // distanza del tiro (cm) -> aggiunto
    private double trajectoryDistance;// distanza reale

    // -------------------------
    // RELEASE METRICS
    // -------------------------
    private double releaseHeight;     // altezza al rilascio (cm)
    private int releaseFrame;         // frame del rilascio

    // -------------------------
    // ARM BIOMECHANICS
    // -------------------------
    private double elbowAngle;        // angolo gomito (°)
    private double wristAngle;        // angolo polso (°)
    private double elbowAlignmentError; // errore allineamento braccio

    // -------------------------
    // BODY BALANCE
    // -------------------------
    private double bodyBalanceScore;  // equilibrio del corpo (0-1)

    // -------------------------
    // SCORING
    // -------------------------
    private double overallScore;      // punteggio complessivo (0-100)

    // -------------------------
    // FEEDBACK
    // -------------------------
    private List<String> suggestions; // suggerimenti per migliorare

    // -------------------------
    // SHOT RESULT
    // -------------------------
    private boolean make;             // tiro riuscito
    private String missType;          // tipo di errore: short/long/center
}