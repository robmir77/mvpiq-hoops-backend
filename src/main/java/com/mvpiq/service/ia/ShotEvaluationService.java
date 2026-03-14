package com.mvpiq.service.ia;

import ai.djl.modality.cv.output.Point;
import com.mvpiq.dto.ShotMetricsDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class ShotEvaluationService {

    private static final Logger LOG = Logger.getLogger(ShotEvaluationService.class);

    @Inject
    HoopDetectionService hoopDetector;

    @Inject
    BallTrackingService aiTracker;

    @Inject
    TrajectoryService trajectoryService;

    @Inject
    ShotMetricsService shotMetricsService;

    @ConfigProperty(name = "mvpiq.video.frame-width")
    int frameWidth;

    @ConfigProperty(name = "mvpiq.video.frame-height")
    int frameHeight;

    @ConfigProperty(name = "mvpiq.hoop.fallback-radius")
    int fallbackRadius;

    @ConfigProperty(name = "mvpiq.hoop.search-frames")
    int searchFrames;

    public JsonObject analyzeShot(List<File> frames) {

        if (frames == null || frames.isEmpty()) {
            throw new RuntimeException("No frames for shot analysis");
        }

        LOG.infof("Starting shot analysis. Frames received: %d", frames.size());

        Hoop hoop = null;
        int framesToCheck = Math.min(searchFrames, frames.size());

        // -------------------------
        // Hoop detection
        // -------------------------
        for (int i = 0; i < framesToCheck; i++) {

            File frame = frames.get(i);

            if (frame == null || !frame.exists()) {
                continue;
            }

            try {

                hoop = hoopDetector.detectHoop(frame);

                if (hoop != null) {
                    LOG.infof("Hoop detected on frame %d", i);
                    break;
                }

            } catch (Exception e) {
                LOG.errorf(e, "Hoop detection failed on frame %d", i);
            }
        }

        int hoopX, hoopY, rimRadius;

        if (hoop != null) {

            hoopX = hoop.x;
            hoopY = hoop.y;
            rimRadius = hoop.radius;

            LOG.infof("Hoop detected -> x=%d y=%d radius=%d", hoopX, hoopY, rimRadius);

        } else {

            LOG.warn("Hoop not detected. Using fallback position");

            hoopX = frameWidth / 2;
            hoopY = frameHeight / 6;
            rimRadius = fallbackRadius;

            LOG.infof("Fallback hoop -> x=%d y=%d radius=%d", hoopX, hoopY, rimRadius);
        }

        // -------------------------
        // Pixel to cm conversion
        // -------------------------
        double hoopDiameterPx = rimRadius * 2;
        double hoopDiameterCm = 45.0;

        double pixelToCm = hoopDiameterCm / hoopDiameterPx;

        LOG.infof("Pixel scale -> 1px = %.3f cm", pixelToCm);

        // -------------------------
        // Ball tracking
        // -------------------------
        List<Point> ballPositions = new ArrayList<>();

        try {

            List<Point> aiPoints = aiTracker.trackBallAI(frames);

            LOG.infof("Raw AI ball detections: %d", aiPoints.size());

            aiPoints = trajectoryService.smoothTrajectory(aiPoints);

            ballPositions.addAll(aiPoints);

        } catch (Exception e) {
            LOG.error("Ball tracking failed", e);
        }

        LOG.infof("Ball trajectory points after smoothing: %d", ballPositions.size());

        ShotMetricsDTO metrics;
        double realTrajectoryDistance = 0.0;

        if (ballPositions.size() < 3) {

            LOG.warn("Too few trajectory points for shot analysis");

            metrics = new ShotMetricsDTO();

        } else {

            // -------------------------
            // Convert trajectory to cm
            // -------------------------
            List<Point> ballCmPositions = ballPositions.stream()
                    .map(p -> new Point(p.getX() * pixelToCm, p.getY() * pixelToCm))
                    .collect(Collectors.toList());

            LOG.info("Trajectory converted to real-world coordinates (cm)");

            // -------------------------
            // Detect release frame
            // -------------------------
            int releaseFrame = shotMetricsService.detectReleaseFrame(ballCmPositions);

            LOG.infof("Estimated release frame: %d", releaseFrame);

            // -------------------------
            // Hoop position in cm
            // -------------------------
            double hoopXCm = hoopX * pixelToCm;
            double hoopYCm = hoopY * pixelToCm;
            double rimRadiusCm = rimRadius * pixelToCm;

            // -------------------------
            // Release point
            // -------------------------
            Point releasePoint = ballCmPositions.get(releaseFrame);

            LOG.infof("Release point -> x=%.2f cm y=%.2f cm",
                    releasePoint.getX(), releasePoint.getY());

            // -------------------------
            // Vertical distance
            // -------------------------
            double verticalDistance = Math.abs(releasePoint.getY() - hoopYCm);

            LOG.infof("Vertical distance from hoop: %.2f cm", verticalDistance);

            // -------------------------
            // Horizontal scaling
            // -------------------------
            double horizontalVideoDistance = Math.abs(releasePoint.getX() - hoopXCm);

            double horizontalRealDistance = 457.0;

            double scaleFactor = horizontalRealDistance / horizontalVideoDistance;

            LOG.infof("Horizontal scaling factor: %.2f", scaleFactor);

            // -------------------------
            // Real trajectory distance
            // -------------------------
            double sum = 0.0;

            for (int i = 1; i < ballCmPositions.size(); i++) {

                Point prev = ballCmPositions.get(i - 1);
                Point curr = ballCmPositions.get(i);

                double dx = curr.getX() - prev.getX();
                double dy = curr.getY() - prev.getY();

                sum += Math.sqrt(dx * dx + dy * dy);
            }

            realTrajectoryDistance = sum * scaleFactor;

            LOG.infof("Estimated real trajectory distance: %.2f cm", realTrajectoryDistance);

            // -------------------------
            // Shot metrics
            // -------------------------
            metrics = shotMetricsService.calculateMetricsVerticalOnly(
                    ballCmPositions,
                    verticalDistance,
                    pixelToCm,
                    hoopYCm,
                    rimRadiusCm
            );

            metrics.setReleaseFrame(releaseFrame);
            metrics.setTrajectoryDistance(realTrajectoryDistance);

            // -------------------------
            // Shot result evaluation
            // -------------------------
            evaluateShotResult(
                    metrics,
                    ballCmPositions,
                    hoopXCm,
                    hoopYCm,
                    rimRadiusCm
            );

            LOG.infof("Shot result -> make=%s missType=%s",
                    metrics.isMake(),
                    metrics.getMissType());

            double shotSpeed = shotMetricsService.estimateShotSpeed(
                    ballCmPositions,
                    releaseFrame,
                    10
            );

            metrics.setShotSpeed(shotSpeed);

            LOG.infof("Estimated shot speed: %.2f km/h", shotSpeed);

            double difficulty = shotMetricsService.calculateShotDifficulty(metrics);
            metrics.setShotDifficulty(difficulty);

            LOG.infof("Shot difficulty: %.2f / 100", difficulty);
        }

        JsonObject result = evaluateShot(metrics);

        LOG.info("Shot evaluation completed");

        return result;
    }

    public JsonObject evaluateShot(ShotMetricsDTO m){

        JsonArrayBuilder errors = Json.createArrayBuilder();
        JsonArrayBuilder suggestions = Json.createArrayBuilder();

        int score = 100;

        if(m.getReleaseAngle() < 42){
            errors.add("Release angle too flat");
            score -= 12;
        }

        if(m.getReleaseAngle() > 60){
            errors.add("Release angle too high");
            score -= 8;
        }

        if(m.getArcHeight() < 80){
            errors.add("Low arc");
            score -= 10;
        }

        if(m.getEntryAngle() < 35){
            errors.add("Flat entry angle");
            score -= 8;
        }

        return Json.createObjectBuilder()
                .add("score", score)
                .add("make", m.isMake())
                .add("missType", m.getMissType() == null ? "" : m.getMissType())
                .add("releaseFrame", m.getReleaseFrame())
                .add("distance", m.getDistance())
                .add("trajectoryDistance", m.getTrajectoryDistance())
                .add("metrics", Json.createObjectBuilder()
                        .add("releaseAngle", m.getReleaseAngle())
                        .add("entryAngle", m.getEntryAngle())
                        .add("releaseSpeed", m.getReleaseSpeed())
                        .add("arcHeight", m.getArcHeight())
                        .add("trajectoryQuality", m.getTrajectoryQuality()))
                .add("errors", errors)
                .add("suggestions", suggestions)
                .add("shotDifficulty", m.getShotDifficulty())
                .build();
    }

    public void evaluateShotResult(
            ShotMetricsDTO metrics,
            List<Point> trajectory,
            double hoopX,
            double hoopY,
            double rimRadiusCm) {

        if (trajectory == null || trajectory.size() < 3) {
            return;
        }

        boolean make = false;
        Point closestPoint = null;
        double minDistance = Double.MAX_VALUE;

        for (Point p : trajectory) {

            double dx = p.getX() - hoopX;
            double dy = p.getY() - hoopY;

            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance < minDistance) {
                minDistance = distance;
                closestPoint = p;
            }

            // se la palla passa dentro il ferro
            if (distance <= rimRadiusCm * 1.25) {
                make = true;
            }
        }

        metrics.setMake(make);

        if (make) {
            metrics.setMissType(null);
            return;
        }

        // se è miss, classifica il tipo di errore
        double dx = closestPoint.getX() - hoopX;
        double dy = closestPoint.getY() - hoopY;

        if (Math.abs(dx) > Math.abs(dy)) {

            metrics.setMissType(dx < 0 ? "LEFT" : "RIGHT");

        } else {

            metrics.setMissType(dy > 0 ? "SHORT" : "LONG");
        }
    }
}