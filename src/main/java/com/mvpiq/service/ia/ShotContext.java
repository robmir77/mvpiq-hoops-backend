package com.mvpiq.service.ia;

import ai.djl.modality.cv.output.Point;
import com.mvpiq.dto.BallPointDTO;
import com.mvpiq.dto.PoseFrameDTO;
import com.mvpiq.dto.ShotMetricsDTO;
import com.mvpiq.enums.HandSide;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.jboss.logging.Logger;

import java.awt.geom.AffineTransform;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
public class ShotContext {

    // =========================
    // 🎬 FRAMES / VIDEO
    // =========================
    public List<File> frames;
    public int fps;

    // dimensioni originali (SERVONO per normalizzazione / debug)
    public int frameWidth;
    public int frameHeight;

    public List<AffineTransform> frameTransforms;
    public boolean stabilized;

    // =========================
    // 🏀 HOOP (SOURCE OF TRUTH = NORMALIZED)
    // =========================
    // coordinate SEMPRE 0–1
    public Hoop hoopNorm;

    // =========================
    // 🏀 BALL (SOURCE OF TRUTH)
    // =========================
    // coordinate SEMPRE 0–1
    public List<BallPointDTO> ballNorm;

    // =========================
    // 🧍 POSE (NORMALIZED)
    // =========================
    public List<PoseFrameDTO> poseFrames;
    public HandSide shootingHand;

    // =========================
    // 🎯 EVENTS (NORMALIZED)
    // =========================
    public int startShotFrame;
    public int releaseFrame;

    // coordinate SEMPRE 0–1
    public Point releaseNorm;

    // =========================
    // 📈 TRAJECTORIES (NORMALIZED ONLY)
    // =========================
    // arco reale (punti filtrati)
    public List<Point> flightArcNorm;

    // parabole (y = ax^2 + bx + c) su coordinate NORMALIZZATE
    public PolynomialFunction realArcNorm;
    public PolynomialFunction idealArcNorm;
    public PolynomialFunction physicsArcNorm;

    // =========================
    // 📊 OUTPUT
    // =========================
    public ShotMetricsDTO metrics;

    public ShotContext(int searchFrames) {
        this.fps = searchFrames;
    }

    // =========================
    // 🧪 DEBUG / VALIDATION
    // =========================

    /**
     * Verifica veloce: tutte le coordinate devono essere tra 0 e 1
     */
    public boolean isNormalized() {

        if (ballNorm != null) {
            for (BallPointDTO p : ballNorm) {
                if (p.getX() > 1 || p.getY() > 1) {
                    return false;
                }
            }
        }

        if (releaseNorm != null) {
            if (releaseNorm.getX() > 1 || releaseNorm.getY() > 1) {
                return false;
            }
        }

        if (flightArcNorm != null) {
            for (Point p : flightArcNorm) {
                if (p.getX() > 1 || p.getY() > 1) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Log utile per debug pipeline
     */
    public void logState(Logger log) {

        log.info("📦 ShotContext state:");
        log.infof("Frames: %d", frames != null ? frames.size() : 0);
        log.infof("Ball points: %d", ballNorm != null ? ballNorm.size() : 0);
        log.infof("Flight arc: %d", flightArcNorm != null ? flightArcNorm.size() : 0);

        if (!isNormalized()) {
            log.warn("⚠️ Context contains NON-normalized data!");
        } else {
            log.info("✅ All coordinates are normalized (0–1)");
        }
    }
}