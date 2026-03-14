package com.mvpiq.service.ai;

import ai.djl.modality.cv.output.Point;
import com.mvpiq.dto.ShotMetrics;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
    ShotMetricsService  shotMetricsService;

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

        LOG.info("Starting shot analysis. Frames received: " + frames.size());

        // -------------------------
        // HOOP DETECTION
        // -------------------------

        Hoop hoop = null;

        int framesToCheck = Math.min(searchFrames, frames.size());

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

        int hoopX;
        int hoopY;
        int rimRadius;

        if (hoop != null) {

            hoopX = hoop.x;
            hoopY = hoop.y;
            rimRadius = hoop.radius;

            LOG.infof("Hoop detected -> x=%d y=%d radius=%d", hoopX, hoopY, rimRadius);

        } else {

            LOG.warn("Hoop not detected. Using fallback");

            hoopX = frameWidth / 2;
            hoopY = frameHeight / 6;
            rimRadius = fallbackRadius;

            LOG.infof("Fallback hoop -> x=%d y=%d radius=%d", hoopX, hoopY, rimRadius);
        }

        // -------------------------
        // SCALE (pixel -> cm)
        // -------------------------

        double hoopDiameterPx = rimRadius * 2;
        double hoopDiameterCm = 45.0;

        double pixelToCm = hoopDiameterCm / hoopDiameterPx;

        LOG.infof("Pixel scale -> 1px = %.3f cm", pixelToCm);

        // -------------------------
        // BALL TRACKING
        // -------------------------

        List<Point> ballPositions = new ArrayList<>();

        try {

            List<Point> aiPoints = aiTracker.trackBallAI(frames);

            aiPoints = trajectoryService.smoothTrajectory(aiPoints);

            ballPositions.addAll(aiPoints);

        } catch (Exception e) {

            LOG.error("Error during ball tracking", e);
        }

        LOG.info("Ball positions detected: " + ballPositions.size());

        // -------------------------
        // CALCOLO METRICHE
        // -------------------------

        ShotMetrics metrics;

        if (ballPositions.size() < 3) {

            LOG.warn("Too few trajectory points");
            metrics = new ShotMetrics();

        } else {

            metrics = shotMetricsService.calculateMetrics(ballPositions, hoopX, hoopY, rimRadius, pixelToCm);
        }

        JsonObject result = evaluateShot(metrics);

        LOG.info("Shot evaluation completed");

        return result;
    }

    public JsonObject evaluateShot(ShotMetrics m){

        JsonArrayBuilder errors=Json.createArrayBuilder();
        JsonArrayBuilder suggestions=Json.createArrayBuilder();

        int score=100;

        if(m.getReleaseAngle()<42){errors.add("Release angle too flat");score-=12;}
        if(m.getReleaseAngle()>60){errors.add("Release angle too high");score-=8;}
        if(m.getArcHeight()<80){errors.add("Low arc");score-=10;}
        if(m.getEntryAngle()<35){errors.add("Flat entry angle");score-=8;}

        return Json.createObjectBuilder()
                .add("score",score)
                .add("make",m.isMake())
                .add("missType",m.getMissType()==null?"":m.getMissType())
                .add("metrics",Json.createObjectBuilder()
                        .add("releaseAngle",m.getReleaseAngle())
                        .add("entryAngle",m.getEntryAngle())
                        .add("releaseSpeed",m.getReleaseSpeed())
                        .add("arcHeight",m.getArcHeight())
                        .add("trajectoryQuality",m.getTrajectoryQuality()))
                .add("errors",errors)
                .add("suggestions",suggestions)
                .build();
    }
}