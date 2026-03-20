package com.mvpiq.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class ShotMetricsDTO {

    // =========================
    // UNIT SYSTEM
    // =========================
    // Tutte le misure spaziali sono NORMALIZZATE (0–1 rispetto al frame)
    // Le velocità sono in unità NORMALIZED / frame
    // Gli angoli sono in GRADI

    // -------------------------
    // BALL TRAJECTORY
    // -------------------------

    private double releaseAngle;
    // Angolo di rilascio (°)
    // Range tipico: 40° – 60°

    private double entryAngle;
    // Angolo d’ingresso nel canestro (°)
    // Range tipico: 35° – 55°

    private double releaseSpeed;
    // Velocità al rilascio (normalized units / frame)
    // Derivata da distanza tra punti consecutivi

    private double arcHeight;
    // Altezza massima della traiettoria (normalized 0–1)
    // Es: 0.25 = 25% altezza frame

    private double trajectoryQuality;
    // Qualità della parabola (0–1)
    // Basata su smoothness / fit ideale

    private double distance;
    // Distanza tiro → ferro (normalized)
    // Tipico: 0.3 (vicino) → 1.2 (long range)

    private double trajectoryDistance;
    // Lunghezza reale della traiettoria (normalized)
    // Somma delle distanze tra punti

    private double trajectoryDeviation;
    // Deviazione dalla traiettoria ideale (normalized)
    // 0 = perfetta, >0.15 = errore significativo

    private double shotSpeed;
    // Velocità media del tiro (normalized/frame)

    private double shotDifficulty;
    // Difficoltà stimata del tiro (0–1 o 0–100 a seconda della logica)


    // -------------------------
    // RELEASE METRICS
    // -------------------------

    private double releaseHeight;
    // Altezza della palla al rilascio (normalized 0–1)

    private int releaseFrame;
    // Frame index del rilascio


    // -------------------------
    // ARM BIOMECHANICS
    // -------------------------

    private double elbowAngle;
    // Angolo del gomito (°)
    // Ideale: ~85° – 100°

    private double wristAngle;
    // Angolo del polso (°)

    private double elbowAlignmentError;
    // Errore di allineamento gomito (normalized o gradi)
    // 0 = perfetto

    private double kneeAngle;
    // Angolo ginocchia (°)

    private double bodyExtension;
    // Estensione del corpo (0–1)
    // 1 = completamente esteso

    private double stabilityScore;
    // Stabilità del tiro (0–1)


    // -------------------------
    // BODY BALANCE
    // -------------------------

    private double bodyBalanceScore;
    // Equilibrio del corpo (0–1)


    // -------------------------
    // SCORING
    // -------------------------

    private double overallScore;
    // Punteggio complessivo (0–100)


    // -------------------------
    // FEEDBACK
    // -------------------------

    private List<String> suggestions;
    // Suggerimenti per migliorare il tiro


    // -------------------------
    // SHOT RESULT
    // -------------------------

    private boolean make;
    // TRUE = canestro segnato

    private String missType;
    // Tipo errore:
    // "SHORT", "LONG", "LEFT", "RIGHT", "UNKNOWN"
}