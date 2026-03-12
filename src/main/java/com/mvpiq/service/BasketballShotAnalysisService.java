package com.mvpiq.service;

import com.mvpiq.dto.ShotMetrics;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;

import org.jboss.logging.Logger;

import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_imgproc.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class BasketballShotAnalysisService {

    private static final Logger LOG = Logger.getLogger(BasketballShotAnalysisService.class);

    /**
     * Analizza il tiro a basket da video già scomposto in frames
     */
    public JsonObject analyzeShot(List<File> frames) throws IOException {

        if (frames == null || frames.isEmpty()) {
            LOG.error("No frames provided for analysis");
            throw new RuntimeException("No frames for shot analysis");
        }

        LOG.info("Starting shot analysis. Frames received: " + frames.size());

        // **Qui usiamo l'AI invece di trackBall**
        List<org.bytedeco.opencv.opencv_core.Point> ballPositions = new ArrayList<>();
        try {
            BallTrackingAI aiTracker = new BallTrackingAI();
            List<java.awt.Point> aiPoints = aiTracker.trackBallAI(frames);

           // Converto in opencv_core.Point
            ballPositions = new ArrayList<>();
            for (java.awt.Point p : aiPoints) {
                ballPositions.add(new org.bytedeco.opencv.opencv_core.Point(p.x, p.y));
            }
        } catch (Exception e) {
            LOG.errorf(e, "Error during AI ball tracking");
        }

        LOG.info("Ball positions detected: " + ballPositions.size());
        for (Point p : ballPositions) {
            LOG.infof("Trajectory point -> x:%d y:%d", p.x(), p.y());
        }

        ShotMetrics metrics = calculateMetrics(ballPositions);

        LOG.infof(
                "Metrics calculated -> releaseAngle=%.2f entryAngle=%.2f speed=%.2f arcHeight=%.2f trajectory=%.2f",
                metrics.releaseAngle,
                metrics.entryAngle,
                metrics.releaseSpeed,
                metrics.arcHeight,
                metrics.trajectoryQuality
        );

        JsonObject result = evaluateShot(metrics);

        LOG.info("Shot evaluation completed");

        return result;
    }

    /**
     * CALCOLO METRICHE SUL TIRO
     */
    public ShotMetrics calculateMetrics(List<Point> points) {

        LOG.info("Calculating shot metrics from " + points.size() + " trajectory points");

        ShotMetrics m = new ShotMetrics();

        if (points.size() < 3) {
            LOG.warn("Not enough points to calculate shot metrics");
            return m;
        }

        Point p0 = points.get(0);
        Point p1 = points.get(1);

        double dx = p1.x() - p0.x();
        double dy = p0.y() - p1.y();

        m.releaseSpeed = Math.sqrt(dx * dx + dy * dy);
        m.releaseAngle = Math.toDegrees(Math.atan2(dy, dx));

        int minY = points.stream().mapToInt(Point::y).min().orElse(p0.y());
        m.arcHeight = p0.y() - minY;

        Point last = points.get(points.size() - 1);
        Point prev = points.get(points.size() - 2);

        double edx = last.x() - prev.x();
        double edy = prev.y() - last.y();
        m.entryAngle = Math.toDegrees(Math.atan2(edy, edx));

        m.trajectoryQuality = evaluateArc(points);

        return m;
    }

    /**
     * VALUTAZIONE DELLA PARABOLA
     */
    private double evaluateArc(List<Point> points) {

        if (points.size() < 5) {
            LOG.info("Not enough points to evaluate trajectory quality");
            return 0;
        }

        double variance = 0;
        for (int i = 1; i < points.size() - 1; i++) {
            int yPrev = points.get(i - 1).y();
            int y = points.get(i).y();
            int yNext = points.get(i + 1).y();
            variance += Math.abs(yPrev - 2 * y + yNext);
        }

        double quality = Math.max(0, 1 - variance / 1000);
        LOG.info("Trajectory quality: " + quality);

        return quality;
    }

    /**
     * VALUTAZIONE DEL TIRO
     */
    public JsonObject evaluateShot(ShotMetrics m) {

        LOG.info("Evaluating shot quality...");

        JsonArrayBuilder errors = Json.createArrayBuilder();
        JsonArrayBuilder suggestions = Json.createArrayBuilder();

        int score = 100;

        if (m.releaseAngle < 42) { errors.add("Release angle too flat"); suggestions.add("Increase shooting arc"); score -= 12; }
        if (m.releaseAngle > 60) { errors.add("Release angle too high"); suggestions.add("Lower arc slightly"); score -= 8; }
        if (m.arcHeight < 80) { errors.add("Low arc"); suggestions.add("Increase arc for softer entry"); score -= 10; }
        if (m.entryAngle < 35) { errors.add("Flat entry angle"); suggestions.add("Shoot with higher arc"); score -= 8; }
        if (m.releaseSpeed < 4) { errors.add("Shot too weak"); suggestions.add("Use more leg power"); score -= 12; }

        LOG.info("Shot score calculated: " + score);

        JsonObject metrics = Json.createObjectBuilder()
                .add("releaseAngle", m.releaseAngle)
                .add("entryAngle", m.entryAngle)
                .add("releaseSpeed", m.releaseSpeed)
                .add("arcHeight", m.arcHeight)
                .add("trajectoryQuality", m.trajectoryQuality)
                .build();

        return Json.createObjectBuilder()
                .add("score", score)
                .add("metrics", metrics)
                .add("errors", errors)
                .add("suggestions", suggestions)
                .build();
    }
}