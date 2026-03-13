package com.mvpiq.service;

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
public class BasketballShotAnalysisService {

    private static final Logger LOG = Logger.getLogger(BasketballShotAnalysisService.class);

    @Inject
    HoopDetectorAI hoopDetector;

    @Inject
    BallTrackingAI aiTracker;

    @ConfigProperty(name = "mvpiq.video.frame-width")
    int frameWidth;

    @ConfigProperty(name = "mvpiq.video.frame-height")
    int frameHeight;

    @ConfigProperty(name = "mvpiq.hoop.fallback-radius")
    int fallbackRadius;

    @ConfigProperty(name = "mvpiq.hoop.search-frames")
    int searchFrames;

    /**
     * Analizza il tiro a basket da video già scomposto in frames
     */
    public JsonObject analyzeShot(List<File> frames) {

        if (frames == null || frames.isEmpty()) {
            LOG.error("No frames provided for analysis");
            throw new RuntimeException("No frames for shot analysis");
        }

        LOG.info("Starting shot analysis. Frames received: " + frames.size());

        // -------------------------
        // HOOP DETECTION
        // -------------------------

        Hoop hoop = null;

        LOG.info("Starting hoop detection");

        // controlla solo i primi frame configurati
        int framesToCheck = Math.min(searchFrames, frames.size());

        for (int i = 0; i < framesToCheck; i++) {

            File frame = frames.get(i);

            if (frame == null || !frame.exists()) {
                LOG.warnf("Skipping invalid frame at index %d", i);
                continue;
            }

            try {

                //hoop = hoopDetector.detectHoop(frame);

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

            LOG.infof(
                    "Hoop detected -> x=%d y=%d radius=%d",
                    hoopX,
                    hoopY,
                    rimRadius
            );

        } else {

            LOG.warn("Hoop not detected. Using fallback hoop position");

            hoopX = frameWidth / 2;
            hoopY = frameHeight / 6;
            rimRadius = fallbackRadius;

            LOG.infof(
                    "Fallback hoop -> x=%d y=%d radius=%d",
                    hoopX,
                    hoopY,
                    rimRadius
            );
        }

        // -------------------------
        // BALL TRACKING (AI)
        // -------------------------
        List<Point> ballPositions = new ArrayList<>();

        try {

            List<Point> aiPoints = aiTracker.trackBallAI(frames);

            for (Point p : aiPoints) {
                ballPositions.add(new Point(p.getX(), p.getY()));
            }

        } catch (Exception e) {

            LOG.errorf(e, "Error during AI ball tracking");
        }

        LOG.info("Ball positions detected: " + ballPositions.size());

        for (Point p : ballPositions) {
            LOG.infof("Trajectory point -> x:%f y:%f", p.getX(), p.getY());
        }

        // -------------------------
        // CALCOLO METRICHE
        // -------------------------
        ShotMetrics metrics = null;
        try {
            if (ballPositions.size() < 3) {
                LOG.warn("Too few points for metrics calculation, returning default metrics");
                metrics = new ShotMetrics();
            } else {
                LOG.info("CALCOLO METRICHE");
                metrics = calculateMetrics(ballPositions, hoopX, hoopY, rimRadius);
            }
        } catch (Exception e) {
            LOG.error("Errore durante il calcolo delle metriche", e);
            metrics = new ShotMetrics(); // default safe
        }

        LOG.infof(
                "Metrics calculated -> releaseAngle=%.2f entryAngle=%.2f speed=%.2f arcHeight=%.2f trajectory=%.2f",
                metrics.getReleaseAngle(),
                metrics.getEntryAngle(),
                metrics.getReleaseSpeed(),
                metrics.getArcHeight(),
                metrics.getTrajectoryQuality()
        );

        JsonObject result = evaluateShot(metrics);

        LOG.info("Shot evaluation completed");

        return result;
    }

    /**
     * CALCOLO METRICHE SUL TIRO
     */
    public ShotMetrics calculateMetrics(List<Point> points, int hoopX, int hoopY, int rimRadius) {

        LOG.info("Calculating shot metrics from " + points.size() + " trajectory points");

        ShotMetrics m = new ShotMetrics();

        if (points.size() < 3) {
            LOG.warn("Not enough points to calculate shot metrics");
            return m;
        }

        // -------------------------
        // FIT DELLA PARABOLA
        // -------------------------
        double[] coeff = fitParabola(points);
        double a = coeff[0];
        double b = coeff[1];
        double c = coeff[2];

        LOG.infof("Parabola coefficients -> a=%.6f b=%.6f c=%.6f", a, b, c);

        // -------------------------
        // RELEASE ANGLE
        // -------------------------
        Point start = points.get(0);
        double releaseSlope = 2 * a * start.getX() + b;
        m.setReleaseAngle(computeAngle(-releaseSlope));
        LOG.infof("Release slope=%.4f angle=%.2f", releaseSlope, m.getReleaseAngle());

        // -------------------------
        // ENTRY ANGLE
        // -------------------------
        Point end = points.get(points.size() - 1);
        double entrySlope = 2 * a * end.getX() + b;
        m.setEntryAngle(computeAngle(-entrySlope));
        LOG.infof("Entry slope=%.4f angle=%.2f", entrySlope, m.getEntryAngle());

        // -------------------------
        // ARC HEIGHT
        // -------------------------
        if (Math.abs(a) > 1e-6) {
            double xv = -b / (2 * a);
            double yv = a * xv * xv + b * xv + c;
            m.setArcHeight(start.getY() - yv);
            LOG.infof("Arc vertex -> x=%.2f y=%.2f arcHeight=%.2f", xv, yv, m.getArcHeight());
        } else {
            LOG.warn("Parabola coefficient 'a' too small, arc height unreliable");
        }

        // -------------------------
        // TRAJECTORY QUALITY
        // -------------------------
        double err = trajectoryError(points, a, b, c);
        m.setTrajectoryQuality(Math.max(0, 1 - err / 50));
        LOG.infof("Trajectory error=%.3f quality=%.3f", err, m.getTrajectoryQuality());

        // -------------------------
        // RELEASE SPEED
        // -------------------------
        if (points.size() > 1) {
            Point p0 = points.get(0);
            Point p1 = points.get(1);
            double dx = p1.getX() - p0.getX();
            double dy = p1.getY() - p0.getY();
            m.setReleaseSpeed(Math.sqrt(dx * dx + dy * dy));
            LOG.infof("Release speed (pixel/frame)=%.2f", m.getReleaseSpeed());
        }

        // -------------------------
        // SHOT PREDICTION
        // -------------------------
        if (hoopX > 0) {
            boolean make = willBallEnterHoop(a, b, c, hoopX, hoopY, rimRadius);
            String missType = shotMissType(a, b, c, hoopX, hoopY, rimRadius);
            m.setMake(make);
            m.setMissType(missType);
            LOG.infof("Shot prediction -> make=%s type=%s", make, missType);
        }

        return m;
    }

    /**
     * VALUTAZIONE DEL TIRO
     */
    public JsonObject evaluateShot(ShotMetrics m) {

        LOG.info("Evaluating shot quality...");

        JsonArrayBuilder errors = Json.createArrayBuilder();
        JsonArrayBuilder suggestions = Json.createArrayBuilder();

        int score = 100;

        if (m.getReleaseAngle() < 42) { errors.add("Release angle too flat"); suggestions.add("Increase shooting arc"); score -= 12; }
        if (m.getReleaseAngle() > 60) { errors.add("Release angle too high"); suggestions.add("Lower arc slightly"); score -= 8; }
        if (m.getArcHeight() < 80) { errors.add("Low arc"); suggestions.add("Increase arc for softer entry"); score -= 10; }
        if (m.getEntryAngle() < 35) { errors.add("Flat entry angle"); suggestions.add("Shoot with higher arc"); score -= 8; }
        if (m.getReleaseSpeed() < 4) { errors.add("Shot too weak"); suggestions.add("Use more leg power"); score -= 12; }

        LOG.info("Shot score calculated: " + score);

        JsonObject metrics = Json.createObjectBuilder()
                .add("releaseAngle", m.getReleaseAngle())
                .add("entryAngle", m.getEntryAngle())
                .add("releaseSpeed", m.getReleaseSpeed())
                .add("arcHeight", m.getArcHeight())
                .add("trajectoryQuality", m.getTrajectoryQuality())
                .build();

        return Json.createObjectBuilder()
                .add("score", score)
                .add("make", m.isMake())
                .add("missType", m.getMissType() == null ? "" : m.getMissType())
                .add("metrics", metrics)
                .add("errors", errors)
                .add("suggestions", suggestions)
                .build();
    }

    // -------------------------
    // PARABOLA FIT
    // -------------------------
    private double[] fitParabola(List<Point> points) {

        int n = points.size();

        double sumX = 0, sumX2 = 0, sumX3 = 0, sumX4 = 0;
        double sumY = 0, sumXY = 0, sumX2Y = 0;

        for (Point p : points) {
            double x = p.getX(), y = p.getY();
            double x2 = x * x;

            sumX += x; sumX2 += x2; sumX3 += x2 * x; sumX4 += x2 * x2;
            sumY += y; sumXY += x * y; sumX2Y += x2 * y;
        }

        double[][] A = {{sumX4, sumX3, sumX2}, {sumX3, sumX2, sumX}, {sumX2, sumX, n}};
        double[] B = {sumX2Y, sumXY, sumY};

        double[] coeffs = solve3x3(A, B);
        LOG.infof("fitParabola result -> a=%.6f b=%.6f c=%.6f", coeffs[0], coeffs[1], coeffs[2]);
        return coeffs;
    }

    private double[] solve3x3(double[][] A, double[] B) {
        double a = A[0][0], b = A[0][1], c = A[0][2];
        double d = A[1][0], e = A[1][1], f = A[1][2];
        double g = A[2][0], h = A[2][1], i = A[2][2];

        double det = a*(e*i - f*h) - b*(d*i - f*g) + c*(d*h - e*g);
        if (Math.abs(det) < 1e-6) return new double[]{0,0,0};

        double A1 = (B[0]*(e*i - f*h) - b*(B[1]*i - f*B[2]) + c*(B[1]*h - e*B[2])) / det;
        double B1 = (a*(B[1]*i - f*B[2]) - B[0]*(d*i - f*g) + c*(d*B[2] - B[1]*g)) / det;
        double C1 = (a*(e*B[2] - B[1]*h) - b*(d*B[2] - B[1]*g) + B[0]*(d*h - e*g)) / det;

        return new double[]{A1, B1, C1};
    }

    private double computeAngle(double slope) {
        return Math.toDegrees(Math.atan(slope));
    }

    private double trajectoryError(List<Point> points, double a, double b, double c) {
        double error = 0;
        for (Point p : points) {
            double predicted = a * p.getX() * p.getX() + b * p.getX() + c;
            error += Math.abs(predicted - p.getY());
        }
        return error / points.size();
    }

    private boolean willBallEnterHoop(double a, double b, double c, int hoopX, int hoopY, int rimRadius) {
        double yBall = a * hoopX * hoopX + b * hoopX + c;
        return Math.abs(yBall - hoopY) < rimRadius;
    }

    private String shotMissType(double a, double b, double c, int hoopX, int hoopY, int rimRadius) {
        double yBall = a * hoopX * hoopX + b * hoopX + c;
        if (yBall > hoopY + rimRadius) return "short";
        if (yBall < hoopY - rimRadius) return "long";
        return "center";
    }
}