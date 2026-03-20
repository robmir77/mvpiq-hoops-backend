package com.mvpiq.service.ia;

import ai.djl.modality.cv.output.Point;
import com.mvpiq.dto.BallPointDTO;
import com.mvpiq.dto.ShotMetricsDTO;
import com.mvpiq.enums.HandSide;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class ShotAnalysisService {

    private static final Logger LOG = Logger.getLogger(ShotAnalysisService.class);

    @ConfigProperty(name = "mvpiq.hoop.search-frames")
    int searchFrames;

    @Inject HoopDetectionService hoopDetector;
    @Inject BallTrackingService aiTracker;
    @Inject TrajectoryService trajectoryService;
    @Inject ShotMetricsService shotMetricsService;
    @Inject OverlayDrawerService overlayService;
    @Inject PoseTrackingService poseTracker;
    @Inject VideoStabilizationService stabilizationService;

    public JsonObject analyzeShot(List<File> frames) {

        validateFrames(frames);

        ShotContext ctx = new ShotContext(searchFrames);
        ctx.frames = frames;

        initFrameSize(ctx);

        trackBall(ctx);
        detectHoop(ctx);
        trackPose(ctx);

        detectShotEvents(ctx);
        resolveShootingHand(ctx);

        buildTrajectories(ctx);

        ctx.metrics = computeMetrics(ctx);

        evaluateShotResult(ctx);

        enrichMetrics(ctx);

        drawOverlay(ctx);

        return evaluateShot(ctx.metrics);
    }

    private void initFrameSize(ShotContext ctx) {
        try {
            BufferedImage first = ImageIO.read(ctx.frames.get(0));
            ctx.frameWidth = first.getWidth();
            ctx.frameHeight = first.getHeight();
        } catch (IOException e) {
            throw new RuntimeException("Cannot read first frame", e);
        }
    }

    // =========================
    // TRAJECTORIES
    // =========================
    private void buildTrajectories(ShotContext ctx) {

        ctx.flightArcNorm = trajectoryService.extractFlightArc(ctx);

        ctx.realArcNorm = trajectoryService.fitTrajectory(ctx);

        ctx.idealArcNorm = shotMetricsService.buildIdealArc(ctx,0.15);

        ctx.physicsArcNorm = trajectoryService.buildPhysicsArc(ctx,0.15);

    }

    public JsonObject evaluateShot(ShotMetricsDTO metrics) {

        // -------------------------
        // VALIDATION
        // -------------------------
        if (metrics == null) {
            LOG.warn("Shot evaluation failed: metrics is null");

            return Json.createObjectBuilder()
                    .add("score", 0)
                    .add("make", false)
                    .add("missType", "UNKNOWN")
                    .add("releaseFrame", 0)
                    .add("distance", 0)
                    .add("trajectoryDistance", 0)
                    .add("trajectoryDeviation", 0)
                    .add("metrics", Json.createObjectBuilder()
                            .add("releaseAngle", 0)
                            .add("entryAngle", 0)
                            .add("releaseSpeed", 0)
                            .add("arcHeight", 0)
                            .add("trajectoryQuality", 0))
                    .add("errors", Json.createArrayBuilder()
                            .add("Shot metrics not available"))
                    .add("suggestions", Json.createArrayBuilder())
                    .add("shotDifficulty", 0)
                    .build();
        }

        // -------------------------
        // INIT
        // -------------------------
        JsonArrayBuilder errors = Json.createArrayBuilder();
        JsonArrayBuilder suggestions = Json.createArrayBuilder();

        int score = 100;

        LOG.infof(
                "Evaluating shot -> releaseAngle=%.2f entryAngle=%.2f arcHeight=%.2f deviation=%.2f",
                metrics.getReleaseAngle(),
                metrics.getEntryAngle(),
                metrics.getArcHeight(),
                metrics.getTrajectoryDeviation()
        );

        // -------------------------
        // RELEASE ANGLE
        // -------------------------
        if (metrics.getReleaseAngle() < 42) {
            errors.add("Release angle too flat");
            score -= 12;
        }

        if (metrics.getReleaseAngle() > 60) {
            errors.add("Release angle too high");
            score -= 8;
        }

        // -------------------------
        // ARC HEIGHT
        // -------------------------
        if (metrics.getArcHeight() < 80) {
            errors.add("Low arc");
            score -= 10;
        }

        // -------------------------
        // ENTRY ANGLE
        // -------------------------
        if (metrics.getEntryAngle() < 35) {
            errors.add("Flat entry angle");
            score -= 8;
        }

        // -------------------------
        // TRAJECTORY DEVIATION
        // -------------------------
        if (metrics.getTrajectoryDeviation() > 40) {
            errors.add("Arc too flat compared to ideal trajectory");
            score -= 10;
        }

        if (metrics.getTrajectoryDeviation() < 15) {
            suggestions.add("Excellent shooting arc");
        }

        // -------------------------
        // SCORE NORMALIZATION
        // -------------------------
        if (score < 0) {
            score = 0;
        }

        LOG.infof(
                "Shot evaluation completed -> score=%d make=%s missType=%s",
                score,
                metrics.isMake(),
                metrics.getMissType()
        );

        // -------------------------
        // BUILD RESULT
        // -------------------------
        return Json.createObjectBuilder()
                .add("score", score)
                .add("make", metrics.isMake())
                .add("missType", metrics.getMissType() == null ? "" : metrics.getMissType())
                .add("releaseFrame", metrics.getReleaseFrame())
                .add("distance", metrics.getDistance())
                .add("trajectoryDistance", metrics.getTrajectoryDistance())
                .add("trajectoryDeviation", metrics.getTrajectoryDeviation())
                .add("metrics", Json.createObjectBuilder()
                        .add("releaseAngle", metrics.getReleaseAngle())
                        .add("entryAngle", metrics.getEntryAngle())
                        .add("releaseSpeed", metrics.getReleaseSpeed())
                        .add("arcHeight", metrics.getArcHeight())
                        .add("trajectoryQuality", metrics.getTrajectoryQuality()))
                .add("errors", errors)
                .add("suggestions", suggestions)
                .add("shotDifficulty", metrics.getShotDifficulty())
                .build();
    }

    public void evaluateShotResult(ShotContext ctx) {

        List<Point> trajectory = ctx.flightArcNorm;

        if (trajectory == null || trajectory.size() < 3) {
            ctx.metrics.setMake(false);
            ctx.metrics.setMissType("UNKNOWN");
            return;
        }

        boolean make = false;

        for (Point p : trajectory) {
            double d = Math.hypot(
                    p.getX() - ctx.hoopNorm.center.getX(),
                    p.getY() - ctx.hoopNorm.center.getY()
            );

            if (d <= ctx.hoopNorm.radius * 1.25) make = true;
        }

        ctx.metrics.setMake(make);
    }

    private void validateFrames(List<File> frames) {
        if (frames == null || frames.isEmpty()) {
            throw new RuntimeException("No frames for shot analysis");
        }
    }

    private void detectHoop(ShotContext ctx) {

        Hoop hoop = detectHoopFromFrames(ctx.frames);

        if (hoop == null && ctx.ballNorm != null && ctx.ballNorm.size() >= 3)
            hoop = predictHoopFromTrajectory(ctx);

        if (hoop == null)
            hoop = buildFallbackHoop(ctx);

        // ✅ SOURCE OF TRUTH UNICA
        ctx.hoopNorm = hoop;

        // (opzionale: log utile)
        LOG.infof("Hoop detected -> x=%.3f y=%.3f r=%.3f",
                hoop.center.getX(), hoop.center.getY(), hoop.radius);

    }

    private Hoop detectHoopFromFrames(List<File> frames) {

        int framesToCheck = Math.min(searchFrames, frames.size());

        for (int i = 0; i < framesToCheck; i++) {

            File frame = frames.get(i);

            try {
                Hoop hoop = hoopDetector.detectHoop(frame);

                if (hoop != null) {
                    LOG.infof(
                            "Hoop detected on frame %d",
                            i
                    );
                    return hoop;
                }

            } catch (Exception e) {
                LOG.errorf(e, "Hoop detection failed on frame %d", i);
            }
        }

        return null;
    }

    private Hoop predictHoopFromTrajectory(ShotContext ctx) {

        if (ctx.ballNorm == null || ctx.ballNorm.size() < 3) {
            LOG.warn("predictHoopFromTrajectory: not enough ball positions");
            return null;
        }

        LOG.infof("predictHoopFromTrajectory: using %d ball points", ctx.ballNorm.size());

        BallPointDTO apex = null;
        BallPointDTO last = null;
        double minY = Double.MAX_VALUE;

        for (BallPointDTO p : ctx.ballNorm) {

            if (p == null) {
                continue;
            }

            if (p.getY() < minY) {
                minY = p.getY();
                apex = p;
            }

            last = p;
        }

        if (apex == null || last == null) {
            LOG.warn("predictHoopFromTrajectory: unable to identify apex or last point");
            return null;
        }

        int predictedX = (int) Math.round(last.getX() * ctx.frameWidth);
        int predictedY = (int) Math.round(
                Math.max(apex.getY() + 0.1, last.getY() - 0.05) * ctx.frameHeight
        );

        int predictedRadius = Math.max(6, Math.min(20,
                (int) (ctx.frameWidth * 0.008)
        ));

        LOG.infof(
                "predictHoopFromTrajectory: predicted hoop x=%d y=%d r=%d",
                predictedX,
                predictedY,
                predictedRadius
        );

        return new Hoop(predictedX, predictedY, predictedRadius, ctx.frameWidth, ctx.frameHeight);
    }

    private Hoop buildFallbackHoop(ShotContext ctx) {

        LOG.warn("buildFallbackHoop: using fallback hoop coordinates");

        Hoop hoop = new Hoop(900, 250, 18, ctx.frameWidth, ctx.frameHeight);

        return hoop;
    }

    // =========================
    // BALL TRACKING
    // =========================
    private void trackBall(ShotContext ctx) {

        try {
            ctx.frameTransforms = stabilizationService.computeTransforms(ctx.frames);
            ctx.stabilized = true;

            List<BallPointDTO> norm = aiTracker.trackBallAI(ctx);

            norm = trajectoryService.smoothTrajectory(norm);
            norm = trajectoryService.filterTrajectory(norm);

            ctx.ballNorm = norm;

        } catch (Exception e) {
            LOG.error("Ball tracking error", e);
            ctx.ballNorm = new ArrayList<>();
        }
    }

    private void trackPose(ShotContext ctx) {
        ctx.poseFrames = poseTracker.trackPose(ctx.frames);
    }

    // =========================
    // EVENTS
    // =========================
    private void detectShotEvents(ShotContext ctx) {

        ctx.startShotFrame = shotMetricsService.detectStartShotFrame(ctx.ballNorm);

        ctx.releaseFrame = shotMetricsService.detectReleaseFrame(ctx);

        ctx.releaseFrame = trajectoryService.stabilizeReleaseFrame(
                ctx.releaseFrame,
                ctx.ballNorm
        );

        normalizeEventFrames(ctx);

        BallPointDTO release = ctx.ballNorm.get(ctx.releaseFrame);

        ctx.releaseNorm = new Point(release.getX(), release.getY());

        // rendering only
        /*ctx.releasePx = new Point(
                release.getX() * ctx.frameWidth,
                release.getY() * ctx.frameHeight
        );*/
    }

    private void normalizeEventFrames(ShotContext ctx) {

        if (ctx.startShotFrame < 0)
            ctx.startShotFrame = Math.max(0, ctx.releaseFrame - 4);

        if (ctx.releaseFrame >= ctx.ballNorm.size())
            ctx.releaseFrame = ctx.ballNorm.size() - 1;
    }

    private void resolveShootingHand(ShotContext ctx) {

        try {
            ctx.shootingHand = poseTracker.estimateShootingHand(ctx);
        } catch (Exception e) {
            ctx.shootingHand = HandSide.RIGHT;
        }
    }

    // =========================
    // METRICS
    // =========================
    private ShotMetricsDTO computeMetrics(ShotContext ctx) {

        if (ctx.flightArcNorm == null || ctx.flightArcNorm.size() < 3)
            return new ShotMetricsDTO();

        double path = shotMetricsService.computePathLength(ctx.flightArcNorm);

        LOG.infof("IDEAL ARC SAMPLE: f(0.5)=%.3f", ctx.idealArcNorm.value(0.5));

        double deviation = shotMetricsService.trajectoryDeviation(
                ctx.flightArcNorm,
                ctx.idealArcNorm
        );

        ShotMetricsDTO m = shotMetricsService.calculateMetrics(ctx);

        m.setReleaseFrame(ctx.releaseFrame);
        m.setTrajectoryDistance(path);
        m.setTrajectoryDeviation(deviation);

        return m;
    }

    private void enrichMetrics(ShotContext ctx) {
        ctx.metrics.setShotSpeed(
                shotMetricsService.estimateShotSpeedNorm(ctx.flightArcNorm, 0, 10)
        );
        ctx.metrics.setShotDifficulty(shotMetricsService.calculateShotDifficulty(ctx.metrics));
    }

    // =========================
    // OVERLAY
    // =========================
    private void drawOverlay(ShotContext ctx) {

        try {
            for (int i = ctx.releaseFrame; i < ctx.frames.size(); i++) {

                BufferedImage img = ImageIO.read(ctx.frames.get(i));
                if (img == null) continue;

                overlayService.drawOverlay(img, ctx, i);

                ImageIO.write(img, "jpg", ctx.frames.get(i));
            }

        } catch (Exception e) {
            LOG.error("Overlay error", e);
        }
    }
}