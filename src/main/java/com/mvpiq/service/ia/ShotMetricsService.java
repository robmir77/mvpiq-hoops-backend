package com.mvpiq.service.ia;

import ai.djl.modality.cv.output.Point;
import com.mvpiq.dto.BallPointDTO;
import com.mvpiq.dto.KeyPointDTO;
import com.mvpiq.dto.PoseFrameDTO;
import com.mvpiq.dto.ShotMetricsDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;

@ApplicationScoped
public class ShotMetricsService {

    @Inject
    PoseTrackingService poseTracking;

    @Inject
    TrajectoryService trajectoryService;

    private static final Logger LOG = Logger.getLogger(ShotMetricsService.class.getName());

    // =========================
    // MAIN METRICS (NORMALIZED ONLY)
    // =========================
    public ShotMetricsDTO calculateMetrics(ShotContext ctx) {

        ShotMetricsDTO m = new ShotMetricsDTO();

        if (ctx.flightArcNorm == null || ctx.flightArcNorm.size() < 3)
            return m;

        PolynomialFunction parabola = trajectoryService.fitTrajectory(ctx);

        double[] coeff = parabola.getCoefficients();
        double a = coeff[2];
        double b = coeff[1];

        Point release = ctx.releaseNorm;
        Point hoop = ctx.hoopNorm.center;

        // =========================
        // DISTANCE (0–1)
        // =========================
        double dx = hoop.getX() - release.getX();
        double dy = hoop.getY() - release.getY();
        double distance = Math.hypot(dx, dy);

        m.setDistance(distance);

        // =========================
        // RELEASE ANGLE (°)
        // =========================
        double releaseSlope = 2 * a * release.getX() + b;
        m.setReleaseAngle(Math.toDegrees(Math.atan(-releaseSlope)));

        // =========================
        // ENTRY ANGLE (°)
        // =========================
        Point end = ctx.flightArcNorm.get(ctx.flightArcNorm.size() - 1);
        double entrySlope = 2 * a * end.getX() + b;

        m.setEntryAngle(Math.toDegrees(Math.atan(-entrySlope)));

        // =========================
        // ARC HEIGHT (0–1)
        // =========================
        if (Math.abs(a) > 1e-6) {
            double apexX = -b / (2 * a);
            double apexY = parabola.value(apexX);

            double arcHeight = Math.abs(release.getY() - apexY);
            m.setArcHeight(arcHeight);
        }

        // =========================
        // TRAJECTORY QUALITY (0–1)
        // =========================
        double err = trajectoryError(ctx.flightArcNorm, parabola);
        m.setTrajectoryQuality(Math.max(0, 1 - err * 5));

        // =========================
        // SPEED (NORMALIZED)
        // =========================
        double speed = estimateShotSpeedNorm(
                ctx.flightArcNorm,
                ctx.releaseFrame,
                ctx.fps
        );

        m.setShotSpeed(speed);

        // =========================
        // PATH LENGTH (NORMALIZED)
        // =========================
        double pathLength = computePathLength(ctx.flightArcNorm);
        m.setTrajectoryDistance(pathLength);

        // =========================
        // DIFFICULTY (0–100)
        // =========================
        m.setShotDifficulty(calculateShotDifficulty(m));

        return m;
    }

    // =========================
    // TRAJECTORY ERROR
    // =========================
    private double trajectoryError(List<Point> points, PolynomialFunction f) {

        double error = 0;

        for (Point p : points) {
            double predicted = f.value(p.getX());
            error += Math.abs(predicted - p.getY());
        }

        return error / points.size();
    }

    // =========================
    // PATH LENGTH (NORMALIZED)
    // =========================
    public double computePathLength(List<Point> trajectory) {

        if (trajectory == null || trajectory.size() < 2) {
            LOG.warn("Not enough points to compute trajectory length");
            return 0;
        }

        double length = 0.0;

        for (int i = 1; i < trajectory.size(); i++) {

            Point a = trajectory.get(i - 1);
            Point b = trajectory.get(i);

            double dx = b.getX() - a.getX();
            double dy = b.getY() - a.getY();

            length += Math.sqrt(dx * dx + dy * dy);
        }

        return length;
    }

    // =========================
    // SPEED (NORMALIZED)
    // =========================
    public double estimateShotSpeedNorm(
            List<Point> trajectory,
            int releaseFrame,
            int fps) {

        if (trajectory == null || trajectory.size() < releaseFrame + 2) {
            return 0;
        }

        int apexIndex = releaseFrame;
        double minY = trajectory.get(releaseFrame).getY();

        for (int i = releaseFrame; i < trajectory.size(); i++) {
            double y = trajectory.get(i).getY();

            if (y < minY) {
                minY = y;
                apexIndex = i;
            }
        }

        if (apexIndex <= releaseFrame) {
            apexIndex = Math.min(releaseFrame + 3, trajectory.size() - 1);
        }

        double distance = 0;

        for (int i = releaseFrame + 1; i <= apexIndex; i++) {

            Point a = trajectory.get(i - 1);
            Point b = trajectory.get(i);

            double dx = b.getX() - a.getX();
            double dy = b.getY() - a.getY();

            distance += Math.sqrt(dx * dx + dy * dy);
        }

        double time = (apexIndex - releaseFrame) / (double) fps;

        if (time == 0) return 0;

        return distance / time;
    }

    // =========================
    // DIFFICULTY (CONSISTENT)
    // =========================
    public double calculateShotDifficulty(ShotMetricsDTO m) {

        double distanceScore = clamp01(m.getDistance());
        double speedScore = clamp01(m.getShotSpeed() / 3.0);   // empirico
        double arcScore = clamp01(m.getArcHeight() / 0.5);     // empirico

        double difficulty =
                distanceScore * 0.6 +
                        speedScore * 0.2 +
                        arcScore * 0.2;

        return difficulty * 100;
    }

    private double clamp01(double v) {
        return Math.max(0, Math.min(1, v));
    }

    // =========================
    // IDEAL ARC
    // =========================
    public PolynomialFunction buildIdealArc(
            ShotContext ctx,
            double baseArcHeight
    ) {

        if (ctx.releaseNorm == null || ctx.hoopNorm.center == null) {
            return null;
        }

        double xr = ctx.releaseNorm.getX();
        double yr = ctx.releaseNorm.getY();

        double xh = ctx.hoopNorm.center.getX();
        double yh = ctx.hoopNorm.center.getY();

        double dx = xh - xr;
        double dy = yh - yr;
        double distance = Math.hypot(dx, dy);

        if (distance < 1e-3) {
            return new PolynomialFunction(new double[]{yr, 0, 0});
        }

        double dynamicArcHeight = baseArcHeight + (distance * 0.25);
        dynamicArcHeight = Math.max(baseArcHeight, Math.min(dynamicArcHeight, baseArcHeight * 2.0));

        double skewFactor = 0.58 + 0.08 * Math.min(1.0, distance);
        double xv = xr + dx * skewFactor;

        double baseHeight = Math.min(yr, yh);
        double yv = Math.max(0.0, baseHeight - dynamicArcHeight);

        double denom = (xr - xv) * (xr - xh) * (xv - xh);

        if (Math.abs(denom) < 1e-6) {
            return new PolynomialFunction(new double[]{yr, 0, 0});
        }

        double a = (xh * (yv - yr) + xv * (yr - yh) + xr * (yh - yv)) / denom;

        double b = (xh * xh * (yr - yv)
                + xv * xv * (yh - yr)
                + xr * xr * (yv - yh)) / denom;

        double c = (xv * xh * (xv - xh) * yr
                + xh * xr * (xh - xr) * yv
                + xr * xv * (xr - xv) * yh) / denom;

        return new PolynomialFunction(new double[]{c, b, a});
    }

    // =========================
    // TRAJECTORY DEVIATION
    // =========================
    public double trajectoryDeviation(
            List<Point> trajectory,
            PolynomialFunction idealArc) {

        double error = 0;

        for (Point p : trajectory) {
            double idealY = idealArc.value(p.getX());
            error += Math.abs(p.getY() - idealY);
        }

        return error / trajectory.size();
    }

    // Viene scartato il palleggio
    public int detectStartShotFrame(List<BallPointDTO> trajectory) {

        if (trajectory == null || trajectory.size() < 8) {
            return 0;
        }

        int n = trajectory.size();

        // ignoriamo la prima parte (palleggio)
        int start = n / 3;

        int bestIndex = start;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (int i = start; i < n - 4; i++) {

            BallPointDTO p0 = trajectory.get(i - 2);
            BallPointDTO p1 = trajectory.get(i - 1);
            BallPointDTO p2 = trajectory.get(i);
            BallPointDTO p3 = trajectory.get(i + 1);
            BallPointDTO p4 = trajectory.get(i + 2);
            BallPointDTO p5 = trajectory.get(i + 3);

            double dy1 = p1.getY() - p0.getY();
            double dy2 = p2.getY() - p1.getY();

            double dy3 = p3.getY() - p2.getY();
            double dy4 = p4.getY() - p3.getY();
            double dy5 = p5.getY() - p4.getY();

            double dx1 = Math.abs(p3.getX() - p2.getX());
            double dx2 = Math.abs(p4.getX() - p3.getX());

            // la palla deve salire per almeno 3 frame
            boolean stableRise =
                    dy3 < 0 &&
                            dy4 < 0 &&
                            dy5 < 0;

            if (!stableRise) {
                continue;
            }

            // deve muoversi verso il canestro
            boolean horizontalMotion =
                    dx1 > 1 || dx2 > 1;

            if (!horizontalMotion) {
                continue;
            }

            // punteggio salita
            double riseScore =
                    (-dy3) +
                            (-dy4) +
                            (-dy5);

            // bonus se prima stava scendendo (transizione)
            double transitionBonus = 0;
            if (dy2 > 0 && dy3 < 0) {
                transitionBonus = 15;
            }

            // penalizzo candidati troppo tardivi
            double latePenalty = i * 0.25;

            double score = riseScore + transitionBonus - latePenalty;

            if (score > bestScore) {
                bestScore = score;
                bestIndex = i;
            }
        }

        return Math.max(0, bestIndex - 1);
    }

    public int detectReleaseFrame(ShotContext ctx) {
        List<BallPointDTO> ballPoints = ctx.ballNorm;
        List<PoseFrameDTO> poses =  ctx.poseFrames;

        if (ballPoints == null || poses == null || ballPoints.size() < 6 || poses.size() < 6) {
            return 0;
        }

        int size = Math.min(ballPoints.size(), poses.size());
        int start = size / 3;

        int bestIndex = start;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (int i = Math.max(start, 1); i < size - 2; i++) {

            BallPointDTO prevBall = ballPoints.get(i - 1);
            BallPointDTO ball = ballPoints.get(i);
            BallPointDTO next1 = ballPoints.get(i + 1);
            BallPointDTO next2 = ballPoints.get(i + 2);

            PoseFrameDTO prevPose = poses.get(i - 1);
            PoseFrameDTO pose = poses.get(i);
            PoseFrameDTO poseNext1 = poses.get(i + 1);
            PoseFrameDTO poseNext2 = poses.get(i + 2);

            KeyPointDTO prevWrist = poseTracking.getWrist(prevPose, ctx.shootingHand);
            KeyPointDTO wrist = poseTracking.getWrist(pose, ctx.shootingHand);
            KeyPointDTO elbow = poseTracking.getElbow(pose, ctx.shootingHand);
            KeyPointDTO shoulder = poseTracking.getShoulder(pose, ctx.shootingHand);
            KeyPointDTO wristNext1 = poseTracking.getWrist(poseNext1, ctx.shootingHand);
            KeyPointDTO wristNext2 = poseTracking.getWrist(poseNext2, ctx.shootingHand);

            if (prevWrist == null || wrist == null || elbow == null || shoulder == null
                    || wristNext1 == null || wristNext2 == null) {
                continue;
            }

            if (!prevWrist.isValid(0.3) || !wrist.isValid(0.3) || !elbow.isValid(0.3)
                    || !shoulder.isValid(0.3) || !wristNext1.isValid(0.3) || !wristNext2.isValid(0.3)) {
                continue;
            }

            double distPrev = PoseMathUtils.distance(prevWrist, prevBall);
            double distNow = PoseMathUtils.distance(wrist, ball);
            double distNext1 = PoseMathUtils.distance(wristNext1, next1);
            double distNext2 = PoseMathUtils.distance(wristNext2, next2);

            double dy1 = next1.getY() - ball.getY();
            double dy2 = next2.getY() - next1.getY();

            boolean ballRising = dy1 < 0 && dy2 < 0;
            if (!ballRising) {
                continue;
            }

            boolean nearHandNow = distNow <= 35;
            if (!nearHandNow) {
                continue;
            }

            boolean separationStartsNow = distNext1 > distNow + 4;
            boolean separationContinues = distNext2 > distNext1 + 2;
            if (!(separationStartsNow && separationContinues)) {
                continue;
            }

            boolean ballAboveShoulder = ball.getY() < shoulder.getY() + 20;
            if (!ballAboveShoulder) {
                continue;
            }

            Double elbowAngle = PoseMathUtils.angle(shoulder, elbow, wrist);

            double elbowScore = 0.0;
            if (elbowAngle != null) {
                if (elbowAngle >= 135) {
                    elbowScore = 1.0;
                } else if (elbowAngle >= 115) {
                    elbowScore = 0.7;
                } else if (elbowAngle >= 95) {
                    elbowScore = 0.4;
                }
            }

            double handContactScore = 0.0;
            if (distNow <= 20) {
                handContactScore = 1.0;
            } else if (distNow <= 28) {
                handContactScore = 0.7;
            } else if (distNow <= 35) {
                handContactScore = 0.4;
            }

            double separationGain = distNext2 - distNow;
            double separationScore = Math.min(Math.max(separationGain, 0.0), 35.0) / 35.0;

            double riseScore = Math.min(((-dy1) + (-dy2)), 24.0) / 24.0;

            double contactTrendScore = 0.0;
            if (distPrev <= distNow + 5) {
                contactTrendScore = 1.0;
            } else {
                contactTrendScore = 0.5;
            }

            double score =
                    (handContactScore * 0.35) +
                            (separationScore * 0.30) +
                            (riseScore * 0.15) +
                            (elbowScore * 0.10) +
                            (contactTrendScore * 0.10);

            if (score > bestScore) {
                bestScore = score;
                bestIndex = i;
            }
        }

        return bestIndex;
    }

    public ShotMetricsDTO computeAll(ShotContext ctx) {

        ShotMetricsDTO m = new ShotMetricsDTO();

        if (ctx.flightArcNorm == null || ctx.flightArcNorm.size() < 3)
            return m;

        double path = computePathLength(ctx.flightArcNorm);

        double deviation = trajectoryDeviation(
                ctx.flightArcNorm,
                ctx.idealArcNorm
        );

        m = calculateMetrics(ctx);

        m.setReleaseFrame(ctx.releaseFrame);
        m.setTrajectoryDistance(path);
        m.setTrajectoryDeviation(deviation);

        return m;
    }

    public void evaluateMakeMiss(ShotContext ctx) {

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

            if (d <= ctx.hoopNorm.radius * 1.25) {
                make = true;
            }
        }

        ctx.metrics.setMake(make);

        if (!make) {
            ctx.metrics.setMissType("MISS"); // 🔥 migliorabile dopo
        }
    }

    public void evaluateShotQuality(ShotContext ctx) {

        ShotMetricsDTO m = ctx.metrics;

        int score = 100;
        List<String> errors = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        if (m.getReleaseAngle() < 42) {
            errors.add("Release angle too flat");
            score -= 12;
        }

        if (m.getReleaseAngle() > 60) {
            errors.add("Release angle too high");
            score -= 8;
        }

        if (m.getArcHeight() < 80) {
            errors.add("Low arc");
            score -= 10;
        }

        if (m.getEntryAngle() < 35) {
            errors.add("Flat entry angle");
            score -= 8;
        }

        if (m.getTrajectoryDeviation() > 40) {
            errors.add("Arc too flat vs ideal");
            score -= 10;
        }

        if (m.getTrajectoryDeviation() < 15) {
            suggestions.add("Excellent shooting arc");
        }

        m.setScore(Math.max(score, 0));
        m.setErrors(errors);
        m.setSuggestions(suggestions);
    }
}