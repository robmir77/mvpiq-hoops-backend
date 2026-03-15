package com.mvpiq.service.ia;

import ai.djl.modality.cv.output.Point;
import com.mvpiq.dto.BallPointDTO;
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
        // Detect hoop nelle prime frame
        // -------------------------
        for (int i = 0; i < framesToCheck; i++) {

            File frame = frames.get(i);
            if (frame == null || !frame.exists()) continue;

            try {

                hoop = hoopDetector.detectHoop(frame);

                if (hoop != null) {
                    LOG.infof("Hoop detected on frame %d -> x=%d y=%d r=%d",
                            i, hoop.x, hoop.y, hoop.radius);
                    break;
                }

            } catch (Exception e) {
                LOG.errorf(e, "Hoop detection failed on frame %d", i);
            }
        }

        // -------------------------
        // Ball tracking
        // -------------------------
        List<BallPointDTO> ballPositions = new ArrayList<>();

        try {

            List<BallPointDTO> aiPoints = aiTracker.trackBallAI(frames);

            LOG.infof("AI tracker detected %d raw points", aiPoints.size());

            aiPoints = trajectoryService.smoothTrajectory(aiPoints);

            LOG.infof("Trajectory smoothed -> %d points", aiPoints.size());

            ballPositions.addAll(aiPoints);

        } catch (Exception e) {
            LOG.error("Error during ball tracking", e);
        }

        LOG.infof("Ball positions used for analysis: %d", ballPositions.size());

        // -------------------------
        // Predict hoop from trajectory se non trovato
        // -------------------------
        if (hoop == null && ballPositions.size() >= 3) {

            LOG.info("Hoop not detected visually. Predicting from trajectory...");

            try {

                Hoop predicted = hoopDetector.predictHoopFromTrajectory(ballPositions);

                if (predicted != null) {

                    hoop = predicted;

                    LOG.infof("Hoop predicted from trajectory -> x=%d y=%d r=%d",
                            hoop.x, hoop.y, hoop.radius);

                } else {

                    LOG.warn("Trajectory prediction returned null");

                }

            } catch (Exception e) {

                LOG.error("Error predicting hoop from trajectory", e);

            }
        }

        // -------------------------
        // Se ancora null usa fallback
        // -------------------------
        int hoopX;
        int hoopY;
        int rimRadius;

        if (hoop != null) {

            hoopX = hoop.x;
            hoopY = hoop.y;
            rimRadius = hoop.radius;

            LOG.infof("Using hoop -> x=%d y=%d r=%d", hoopX, hoopY, rimRadius);

        } else {

            LOG.warn("Hoop not detected nor predicted. Using fallback");

            hoopX = frameWidth / 2;
            hoopY = frameHeight / 6;
            rimRadius = fallbackRadius;

            LOG.infof("Fallback hoop -> x=%d y=%d radius=%d", hoopX, hoopY, rimRadius);
        }

        // -------------------------
        // Calcolo pixel -> cm
        // -------------------------
        double hoopDiameterPx = rimRadius * 2;
        double hoopDiameterCm = 45.0;

        double pixelToCm = hoopDiameterCm / hoopDiameterPx;

        LOG.infof("Pixel scale -> 1px = %.3f cm", pixelToCm);

        // -------------------------
        // Metriche
        // -------------------------
        ShotMetricsDTO metrics;
        double realTrajectoryDistance = 0.0;

        if (ballPositions.size() < 3) {

            LOG.warn("Too few trajectory points. Cannot compute metrics.");

            metrics = new ShotMetricsDTO();

        } else {

            // -------------------------
            // Convertiamo pixel -> cm
            // -------------------------
            List<Point> ballCmPositions = ballPositions.stream()
                    .map(p -> new Point(
                            p.getX() * pixelToCm,
                            p.getY() * pixelToCm))
                    .collect(Collectors.toList());

            LOG.infof("Trajectory converted to cm. Points: %d", ballCmPositions.size());

            // -------------------------
            // Trova frame rilascio
            // -------------------------
            int releaseFrame = shotMetricsService.detectReleaseFrame(ballCmPositions);

            LOG.infof("Estimated release frame: %d", releaseFrame);

            if (releaseFrame < 0 || releaseFrame >= ballCmPositions.size()) {
                LOG.warn("Invalid release frame detected. Using frame 0.");
                releaseFrame = 0;
            }

            // -------------------------
            // Coordinate canestro
            // -------------------------
            double hoopXCm = hoopX * pixelToCm;
            double hoopYCm = hoopY * pixelToCm;
            double rimRadiusCm = rimRadius * pixelToCm;

            LOG.infof("Hoop in cm -> x=%.2f y=%.2f radius=%.2f",
                    hoopXCm, hoopYCm, rimRadiusCm);

            // -------------------------
            // Punto di rilascio
            // -------------------------
            Point releasePoint = ballCmPositions.get(releaseFrame);

            LOG.infof("Release point -> x=%.2f y=%.2f",
                    releasePoint.getX(), releasePoint.getY());

            // -------------------------
            // distanza verticale
            // -------------------------
            double verticalDistance = Math.abs(releasePoint.getY() - hoopYCm);

            LOG.infof("Vertical release distance: %.2f cm", verticalDistance);

            // -------------------------
            // stima scala orizzontale
            // -------------------------
            Point hoopPoint = new Point(hoopXCm, hoopYCm);

            double horizontalVideoDistance =
                    Math.abs(releasePoint.getX() - hoopPoint.getX());

            double horizontalRealDistance = 457.0;

            double scaleFactor = 1.0;

            if (horizontalVideoDistance > 0.01) {

                scaleFactor = horizontalRealDistance / horizontalVideoDistance;

            } else {

                LOG.warn("Horizontal video distance too small. Using scaleFactor=1");
            }

            LOG.infof("Scale factor estimated: %.2f", scaleFactor);

            // -------------------------
            // distanza reale traiettoria
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
            // calcolo metriche tiro
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
            // risultato tiro
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

        LOG.infof("Estimated vertical distance: %.2f cm", metrics.getDistance());
        LOG.infof("Estimated real trajectory distance: %.2f cm", realTrajectoryDistance);

        metrics.setTrajectoryDistance(realTrajectoryDistance);

        JsonObject result = evaluateShot(metrics);

        LOG.infof("Shot evaluation completed. Score=%s",
                result.getInt("score"));

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