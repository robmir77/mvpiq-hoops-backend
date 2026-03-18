package com.mvpiq.service.ia;

import ai.djl.modality.cv.output.Point;
import com.mvpiq.dto.BallPointDTO;
import com.mvpiq.dto.PoseFrameDTO;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.awt.geom.Point2D.distance;

@ApplicationScoped
public class ShotAnalysisService {

    private static final Logger LOG = Logger.getLogger(ShotAnalysisService.class);

    @Inject HoopDetectionService hoopDetector;
    @Inject BallTrackingService aiTracker;
    @Inject TrajectoryService trajectoryService;
    @Inject ShotMetricsService shotMetricsService;
    @Inject OverlayDrawerService overlayService;
    @Inject PoseTrackingService poseTracker;

    @ConfigProperty(name = "mvpiq.hoop.search-frames")
    int searchFrames;

    public JsonObject analyzeShot(List<File> frames) {
        validateFrames(frames);

        ShotContext ctx = new ShotContext();
        ctx.frames = frames;

        detectHoop(ctx);
        trackBall(ctx);
        trackPose(ctx);
        computeScale(ctx);
        detectShotEvents(ctx);
        resolveShootingHand(ctx);
        buildFlightTrajectories(ctx);
        fitTrajectories(ctx);

        ShotMetricsDTO metrics = computeMetrics(ctx);
        ctx.metrics = metrics;

        evaluateShotResult(metrics, ctx.flightArcCm, ctx.hoopXCm, ctx.hoopYCm, ctx.rimRadiusCm);
        enrichMetrics(ctx, metrics);
        drawOverlay(ctx);

        return evaluateShot(metrics);
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

    public void evaluateShotResult(
            ShotMetricsDTO metrics,
            List<Point> trajectory,
            double hoopX,
            double hoopY,
            double rimRadiusCm) {

        // -------------------------
        // VALIDATION
        // -------------------------
        if (metrics == null) {
            LOG.warn("Shot result evaluation skipped: metrics is null");
            return;
        }

        if (trajectory == null || trajectory.size() < 3) {
            LOG.warn("Shot result evaluation skipped: not enough trajectory points");
            metrics.setMake(false);
            metrics.setMissType("UNKNOWN");
            return;
        }

        // -------------------------
        // FIND CLOSEST POINT TO HOOP
        // -------------------------
        boolean make = false;
        Point closestPoint = null;
        double minDistance = Double.MAX_VALUE;

        for (Point p : trajectory) {

            double pointDistance = distance(p.getX(), p.getY(), hoopX, hoopY);

            if (pointDistance < minDistance) {
                minDistance = pointDistance;
                closestPoint = p;
            }

            if (pointDistance <= rimRadiusCm * 1.25) {
                make = true;
            }
        }

        metrics.setMake(make);

        LOG.infof("Shot result evaluation -> closestDistance=%.2f cm make=%s", minDistance, make);

        // -------------------------
        // MAKE
        // -------------------------
        if (make) {
            metrics.setMissType(null);
            LOG.info("Shot classified as MAKE");
            return;
        }

        // -------------------------
        // MISS CLASSIFICATION
        // -------------------------
        if (closestPoint == null) {
            LOG.warn("Miss classification failed: closestPoint is null");
            metrics.setMissType("UNKNOWN");
            return;
        }

        double dx = closestPoint.getX() - hoopX;
        double dy = closestPoint.getY() - hoopY;

        if (Math.abs(dx) > Math.abs(dy)) {
            metrics.setMissType(dx < 0 ? "LEFT" : "RIGHT");
        } else {
            metrics.setMissType(dy > 0 ? "SHORT" : "LONG");
        }

        LOG.infof(
                "Shot classified as MISS -> missType=%s dx=%.2f dy=%.2f",
                metrics.getMissType(),
                dx,
                dy
        );
    }

    private void validateFrames(List<File> frames) {
        if (frames == null || frames.isEmpty()) {
            throw new RuntimeException("No frames for shot analysis");
        }
    }

    private void detectHoop(ShotContext ctx) {
        Hoop hoop = detectHoopFromFrames(ctx.frames);

        if (hoop == null && ctx.ballPositions != null && ctx.ballPositions.size() >= 3) {
            hoop = predictHoopFromTrajectory(ctx.ballPositions);
        }

        if (hoop == null) {
            hoop = buildFallbackHoop();
        }

        ctx.hoop = hoop;
        ctx.hoopX = hoop.x;
        ctx.hoopY = hoop.y;
        ctx.rimRadius = hoop.radius;
    }

    private Hoop detectHoopFromFrames(List<File> frames) {

        int framesToCheck = Math.min(searchFrames, frames.size());

        for (int i = 0; i < framesToCheck; i++) {

            File frame = frames.get(i);

            try {
                Hoop hoop = hoopDetector.detectHoop(frame);

                if (hoop != null) {
                    LOG.infof(
                            "Hoop detected on frame %d -> x=%d y=%d r=%d",
                            i,
                            hoop.x,
                            hoop.y,
                            hoop.radius
                    );
                    return hoop;
                }

            } catch (Exception e) {
                LOG.errorf(e, "Hoop detection failed on frame %d", i);
            }
        }

        return null;
    }

    private Hoop predictHoopFromTrajectory(List<BallPointDTO> ballPositions) {

        if (ballPositions == null || ballPositions.size() < 3) {
            LOG.warn("predictHoopFromTrajectory: not enough ball positions");
            return null;
        }

        LOG.infof("predictHoopFromTrajectory: using %d ball points", ballPositions.size());

        BallPointDTO apex = null;
        BallPointDTO last = null;
        double minY = Double.MAX_VALUE;

        for (BallPointDTO p : ballPositions) {

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

        int predictedX = (int) Math.round(last.getX());
        int predictedY = (int) Math.round(Math.max(apex.getY() + 40, last.getY() - 20));
        int predictedRadius = 18;

        LOG.infof(
                "predictHoopFromTrajectory: predicted hoop x=%d y=%d r=%d",
                predictedX,
                predictedY,
                predictedRadius
        );

        return new Hoop(predictedX, predictedY, predictedRadius);
    }

    private Hoop buildFallbackHoop() {
        LOG.warn("buildFallbackHoop: using fallback hoop coordinates");
        return new Hoop(900, 250, 18);
    }

    private void trackBall(ShotContext ctx) {

        try {
            VideoStabilizationService stabilizationService = new VideoStabilizationService();

            ctx.frameTransforms = stabilizationService.computeTransforms(ctx.frames);
            ctx.stabilized = true;

            List<BallPointDTO> aiPoints = aiTracker.trackBallAI(ctx);

            aiPoints = trajectoryService.smoothTrajectory(aiPoints);
            aiPoints = trajectoryService.filterTrajectory(aiPoints);

            ctx.ballPositions = aiPoints;

            ctx.ballPixelPositions = aiPoints.stream()
                    .map(p -> new Point(p.getX(), p.getY()))
                    .toList();

        } catch (Exception e) {
            LOG.error("Error during ball tracking", e);
            ctx.ballPositions = new ArrayList<>();
            ctx.ballPixelPositions = new ArrayList<>();
        }
    }

    private void trackPose(ShotContext ctx) {
        ctx.poseFrames = poseTracker.trackPose(ctx.frames);
    }

    private void computeScale(ShotContext ctx) {
        double hoopDiameterPx = ctx.rimRadius * 2.0;
        double hoopDiameterCm = 45.0;

        ctx.pixelToCm = hoopDiameterCm / hoopDiameterPx;
        ctx.hoopXCm = ctx.hoopX * ctx.pixelToCm;
        ctx.hoopYCm = ctx.hoopY * ctx.pixelToCm;
        ctx.rimRadiusCm = ctx.rimRadius * ctx.pixelToCm;

        ctx.ballCmPositions = ctx.ballPositions.stream()
                .map(p -> new Point(p.getX() * ctx.pixelToCm, p.getY() * ctx.pixelToCm))
                .collect(Collectors.toList());
    }

    private void detectShotEvents(ShotContext ctx) {
        ctx.startShotFrame = shotMetricsService.detectStartShotFrame(ctx.ballPositions);

        ctx.releaseFrame = shotMetricsService.detectReleaseFrame(
                ctx.ballPositions,
                ctx.poseFrames
        );

        ctx.releaseFrame = trajectoryService.stabilizeReleaseFrame(
                ctx.releaseFrame,
                ctx.ballCmPositions
        );

        normalizeEventFrames(ctx);
        resolveEventPoints(ctx);
    }

    private void normalizeEventFrames(ShotContext ctx) {
        if (ctx.startShotFrame < 0) {
            ctx.startShotFrame = Math.max(0, ctx.releaseFrame - 4);
        }

        if (ctx.releaseFrame < ctx.startShotFrame) {
            ctx.releaseFrame = ctx.startShotFrame;
        }

        if (ctx.releaseFrame >= ctx.ballCmPositions.size()) {
            ctx.releaseFrame = ctx.ballCmPositions.size() - 1;
        }
    }

    private void resolveEventPoints(ShotContext ctx) {
        ctx.startShotPointCm = ctx.ballCmPositions.get(ctx.startShotFrame);
        ctx.releasePointCm = ctx.ballCmPositions.get(ctx.releaseFrame);

        ctx.startShotPointPx = ctx.ballPixelPositions.get(ctx.startShotFrame);
        ctx.releasePointPx = ctx.ballPixelPositions.get(ctx.releaseFrame);
    }

    private void resolveShootingHand(ShotContext ctx) {
        try {
            ctx.shootingHand = poseTracker.estimateShootingHand(
                    ctx.ballPositions,
                    ctx.poseFrames,
                    Math.max(0, ctx.releaseFrame - 3),
                    ctx.releaseFrame
            );
        } catch (Exception e) {
            LOG.warn("Unable to resolve shooting hand, fallback to RIGHT", e);
            ctx.shootingHand = HandSide.RIGHT;
        }

        if (ctx.shootingHand == null) {
            ctx.shootingHand = HandSide.RIGHT;
        }
    }

    private void buildFlightTrajectories(ShotContext ctx) {
        ctx.flightArcCm = trajectoryService.extractFlightArc(
                ctx.ballCmPositions,
                ctx.releaseFrame,
                ctx.hoopYCm
        );

        ctx.flightArcPx = trajectoryService.extractFlightArc(
                ctx.ballPixelPositions,
                ctx.releaseFrame,
                ctx.hoopY
        );
    }

    private void fitTrajectories(ShotContext ctx) {
        if (ctx.flightArcCm != null && ctx.flightArcCm.size() >= 3) {
            ctx.realArcCm = shotMetricsService.fitTrajectory(ctx.flightArcCm);
        }

        if (ctx.flightArcPx != null && ctx.flightArcPx.size() >= 3) {
            ctx.realArcPx = shotMetricsService.fitTrajectory(ctx.flightArcPx);
        }

        Point hoopPointCm = new Point(ctx.hoopXCm, ctx.hoopYCm);
        Point hoopPointPx = new Point(ctx.hoopX, ctx.hoopY);

        ctx.idealArcCm = shotMetricsService.buildIdealArc(
                ctx.releasePointCm,
                hoopPointCm,
                120
        );

        ctx.idealArcPx = shotMetricsService.buildIdealArc(
                ctx.releasePointPx,
                hoopPointPx,
                120
        );
    }

    private ShotMetricsDTO computeMetrics(ShotContext ctx) {
        if (ctx.flightArcCm == null || ctx.flightArcCm.size() < 3) {
            ShotMetricsDTO empty = new ShotMetricsDTO();
            empty.setReleaseFrame(ctx.releaseFrame);
            return empty;
        }

        double verticalDistance = Math.abs(ctx.releasePointCm.getY() - ctx.hoopYCm);

        double trajectoryDistance = shotMetricsService.computePathLength(ctx.flightArcCm);

        double trajectoryDeviation = shotMetricsService.trajectoryDeviation(
                ctx.flightArcCm,
                ctx.idealArcCm
        );

        ShotMetricsDTO metrics = shotMetricsService.calculateMetricsVerticalOnly(
                ctx.flightArcCm,
                verticalDistance,
                ctx.pixelToCm,
                ctx.hoopYCm,
                ctx.rimRadiusCm
        );

        metrics.setReleaseFrame(ctx.releaseFrame);
        metrics.setTrajectoryDistance(trajectoryDistance);
        metrics.setTrajectoryDeviation(trajectoryDeviation);

        return metrics;
    }

    private void enrichMetrics(ShotContext ctx, ShotMetricsDTO metrics) {
        double shotSpeed = shotMetricsService.estimateShotSpeed(
                ctx.flightArcCm,
                0,
                10
        );
        metrics.setShotSpeed(shotSpeed);

        double difficulty = shotMetricsService.calculateShotDifficulty(metrics);
        metrics.setShotDifficulty(difficulty);
    }

    private void drawOverlay(ShotContext ctx) {
        try {
            for (int i = ctx.releaseFrame; i < ctx.frames.size(); i++) {

                File frameFile = ctx.frames.get(i);
                BufferedImage img = ImageIO.read(frameFile);

                if (img == null) {
                    continue;
                }

                List<Point> partialTrajectory = ctx.ballPixelPositions.subList(
                        0,
                        Math.min(i + 1, ctx.ballPixelPositions.size())
                );

                PoseFrameDTO pose = null;
                if (ctx.poseFrames != null && i < ctx.poseFrames.size()) {
                    pose = ctx.poseFrames.get(i);
                }

                overlayService.drawOverlay(
                        img,
                        partialTrajectory,
                        ctx.realArcPx,
                        ctx.idealArcPx,
                        ctx.releasePointPx,
                        new Point(ctx.hoopX, ctx.hoopY),
                        ctx.rimRadius,
                        pose,
                        ctx.metrics
                );

                ImageIO.write(img, "jpg", frameFile);
            }
        } catch (Exception e) {
            LOG.error("Error drawing trajectory overlay", e);
        }
    }
}

/*
@ApplicationScoped
public class ShotAnalysisServiceOld {

    private static final Logger LOG = Logger.getLogger(ShotAnalysisService.class);

    @Inject
    HoopDetectionService hoopDetector;

    @Inject
    BallTrackingService aiTracker;

    @Inject
    TrajectoryService trajectoryService;

    @Inject
    ShotMetricsService shotMetricsService;

    @Inject
    OverlayDrawerService overlayService;
    @Inject
    PoseTrackingService poseTracker;

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

            // NUOVO: filtro traiettoria
            aiPoints = trajectoryService.filterTrajectory(aiPoints);

            LOG.infof("Trajectory filtered -> %d points", aiPoints.size());

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
            List<PoseFrameDTO> poseFrames = poseTracker.trackPose(frames);

            int releaseFrame = shotMetricsService.detectReleaseFrame(
                    ballPositions,
                    poseFrames
            );

            releaseFrame = trajectoryService.stabilizeReleaseFrame(releaseFrame, ballCmPositions);

            LOG.infof("Estimated release frame: %d", releaseFrame);

            // -------------------------
            // Coordinate canestro
            // -------------------------
            double hoopXCm = hoopX * pixelToCm;
            double hoopYCm = hoopY * pixelToCm;
            double rimRadiusCm = rimRadius * pixelToCm;

            LOG.infof("Hoop in cm -> x=%.2f y=%.2f radius=%.2f",
                    hoopXCm, hoopYCm, rimRadiusCm);

            Point releasePoint = ballCmPositions.get(releaseFrame);

            LOG.infof("Release point -> x=%.2f y=%.2f",
                    releasePoint.getX(), releasePoint.getY());

            // -------------------------
            // Estrazione arco reale tiro
            // -------------------------
            List<Point> shotArc =
                    trajectoryService.extractShotArc(ballCmPositions, hoopYCm);

            LOG.infof("Shot arc extracted -> %d points", shotArc.size());

            // -------------------------
            // Shot arc in PIXEL (per overlay)
            // -------------------------
            List<Point> ballPixelPositions = ballPositions.stream()
                    .map(p -> new Point(p.getX(), p.getY()))
                    .collect(Collectors.toList());

            // FIX: per overlay usiamo tutta la traiettoria pixel (più stabile per il fit)
            List<Point> shotArcPx = new ArrayList<>(ballPixelPositions);

            LOG.infof("Shot arc pixel points (overlay): %d", shotArcPx.size());

            // -------------------------
            // PARABOLE (REAL vs IDEAL)
            // -------------------------
            // parabola reale per metriche (cm)
            PolynomialFunction realArc =
                    shotMetricsService.fitTrajectory(shotArc);

            // parabola reale per overlay (pixel)
            PolynomialFunction realArcPx = null;

            if (shotArcPx.size() >= 3) {

                realArcPx = shotMetricsService.fitTrajectory(shotArcPx);

            } else {

                LOG.warn("Not enough pixel points to fit realArcPx");

            }

            Point hoopPoint = new Point(hoopXCm, hoopYCm);
            Point hoopPointPx = new Point(hoopX, hoopY);

            PolynomialFunction idealArc =
                    shotMetricsService.buildIdealArc(
                            releasePoint,
                            hoopPoint,
                            120
                    );

            PolynomialFunction idealArcPx = null;

            if (ballPixelPositions.size() > releaseFrame + 2) {

                idealArcPx = shotMetricsService.buildIdealArc(
                        ballPixelPositions.get(releaseFrame),
                        hoopPointPx,
                        120
                );

            } else {

                LOG.warn("Not enough points to build idealArcPx");

            }

            // -------------------------
            // deviazione dalla parabola ideale
            // -------------------------
            double trajectoryDeviation =
                    shotMetricsService.trajectoryDeviation(
                            shotArc,
                            idealArc
                    );

            LOG.infof("Trajectory deviation from ideal arc: %.2f cm", trajectoryDeviation);

            // -------------------------
            // Disegno overlay
            // -------------------------
            try {

                // traiettoria in pixel (serve per disegnare sull'immagine)
                List<Point> pixelTrajectory = ballPositions.stream()
                        .map(p -> new Point(p.getX(), p.getY()))
                        .collect(Collectors.toList());

                Point releasePointPx = pixelTrajectory.size() > releaseFrame
                        ? pixelTrajectory.get(releaseFrame)
                        : pixelTrajectory.get(0);

                LOG.infof("Drawing overlay using pixel coordinates. ReleasePx -> x=%.2f y=%.2f",
                        releasePointPx.getX(),
                        releasePointPx.getY());

                for (int i = releaseFrame; i < frames.size(); i++) {

                    File frameFile = frames.get(i);

                    BufferedImage img = ImageIO.read(frameFile);

                    if (img == null) {
                        LOG.warn("Frame image null: " + frameFile.getName());
                        continue;
                    }

                    List<Point> partialTrajectory =
                            pixelTrajectory.subList(
                                    0,
                                    Math.min(i, pixelTrajectory.size())
                            );

                    overlayService.drawOverlay(
                            img,
                            partialTrajectory,
                            realArcPx,
                            idealArcPx,
                            releasePointPx,
                            hoopPointPx,
                            rimRadius
                    );

                    ImageIO.write(img, "jpg", frameFile);
                }

                LOG.info("Trajectory overlay successfully drawn on frames");

            } catch (Exception e) {

                LOG.error("Error drawing trajectory overlay", e);
            }

            // -------------------------
            // distanza verticale
            // -------------------------
            double verticalDistance = Math.abs(releasePoint.getY() - hoopYCm);

            LOG.infof("Vertical release distance: %.2f cm", verticalDistance);

            // -------------------------
            // distanza reale traiettoria
            // -------------------------
            double sum = 0.0;

            for (int i = 1; i < shotArc.size(); i++) {

                Point prev = shotArc.get(i - 1);
                Point curr = shotArc.get(i);

                double dx = curr.getX() - prev.getX();
                double dy = curr.getY() - prev.getY();

                sum += Math.sqrt(dx * dx + dy * dy);
            }

            realTrajectoryDistance = sum;

            LOG.infof("Estimated real trajectory distance: %.2f cm", realTrajectoryDistance);

            // -------------------------
            // calcolo metriche tiro
            // -------------------------
            metrics = shotMetricsService.calculateMetricsVerticalOnly(
                    shotArc,
                    verticalDistance,
                    pixelToCm,
                    hoopYCm,
                    rimRadiusCm
            );

            metrics.setReleaseFrame(releaseFrame);
            metrics.setTrajectoryDistance(realTrajectoryDistance);
            metrics.setTrajectoryDeviation(trajectoryDeviation);

            // -------------------------
            // risultato tiro
            // -------------------------
            evaluateShotResult(
                    metrics,
                    shotArc,
                    hoopXCm,
                    hoopYCm,
                    rimRadiusCm
            );

            LOG.infof("Shot result -> make=%s missType=%s",
                    metrics.isMake(),
                    metrics.getMissType());

            double shotSpeed = shotMetricsService.estimateShotSpeed(
                    shotArc,
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

        if(m.getTrajectoryDeviation() > 40){
            errors.add("Arc too flat compared to ideal trajectory");
            score -= 10;
        }

        if(m.getTrajectoryDeviation() < 15){
            suggestions.add("Excellent shooting arc");
        }

        return Json.createObjectBuilder()
                .add("score", score)
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
} */