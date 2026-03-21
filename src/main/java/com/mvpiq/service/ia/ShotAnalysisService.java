package com.mvpiq.service.ia;

import ai.djl.modality.cv.output.Point;
import com.mvpiq.dto.BallPointDTO;
import com.mvpiq.dto.ShotMetricsDTO;
import com.mvpiq.enums.HandSide;
import com.mvpiq.enums.ShotType;
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
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class ShotAnalysisService {

    private static final Logger LOG = Logger.getLogger(ShotAnalysisService.class);

    @ConfigProperty(name = "mvpiq.hoop.search-frames")
    int fps;

    @Inject HoopDetectionService hoopDetector;
    @Inject BallTrackingService aiTracker;
    @Inject TrajectoryService trajectoryService;
    @Inject ShotMetricsService shotMetricsService;
    @Inject OverlayDrawerService overlayService;
    @Inject PoseTrackingService poseTracker;
    @Inject VideoStabilizationService stabilizationService;

    public JsonObject analyzeShot(List<File> frames) {

        validateFrames(frames);

        ShotContext ctx = new ShotContext(frames, fps, ShotType.MID_COURT);

        ctx.initFrameSize(LOG);
        ctx.initScale(LOG);

        trackBall(ctx);
        detectHoop(ctx);

        trackPose(ctx);

        detectShotEvents(ctx);
        resolveShootingHand(ctx);

        buildTrajectories(ctx);

        ctx.metrics = shotMetricsService.computeAll(ctx);

        shotMetricsService.evaluateMakeMiss(ctx);
        shotMetricsService.evaluateShotQuality(ctx);

        enrichMetrics(ctx);
        drawOverlay(ctx);

        ctx.logState(LOG);

        return buildJson(ctx);
    }

    private JsonObject buildJson(ShotContext ctx) {

        ShotMetricsDTO m = ctx.metrics;

        JsonArrayBuilder errors = Json.createArrayBuilder();
        m.getErrors().forEach(errors::add);

        JsonArrayBuilder suggestions = Json.createArrayBuilder();
        m.getSuggestions().forEach(suggestions::add);

        return Json.createObjectBuilder()
                .add("score", m.getScore())
                .add("make", m.isMake())
                .add("missType", m.getMissType() == null ? "" : m.getMissType())
                .add("releaseFrame", m.getReleaseFrame())
                .add("distance", m.getDistance())
                .add("trajectoryDistance", m.getTrajectoryDistance())
                .add("trajectoryDeviation", m.getTrajectoryDeviation())
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

    // =========================
    // TRAJECTORIES
    // =========================
    private void buildTrajectories(ShotContext ctx) {

        ctx.flightArcNorm = trajectoryService.extractFlightArc(ctx);

        ctx.realArcNorm = trajectoryService.fitTrajectory(ctx);

        ctx.idealArcNorm = shotMetricsService.buildIdealArc(ctx,0.15);

        ctx.physicsArcNorm = trajectoryService.buildRealPhysicsArc(ctx); // 🔥 sweet spot NBA

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

        int framesToCheck = Math.min(fps, frames.size());

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