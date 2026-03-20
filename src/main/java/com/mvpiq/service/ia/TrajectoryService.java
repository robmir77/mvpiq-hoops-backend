package com.mvpiq.service.ia;

import ai.djl.modality.cv.output.Point;
import com.mvpiq.dto.BallPointDTO;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class TrajectoryService {

    private static final Logger LOG = Logger.getLogger(TrajectoryService.class);

    // NORMALIZED (0–1)
    private static final double MAX_POINT_JUMP = 0.15;
    private static final double DUPLICATE_THRESHOLD = 0.001;
    private static final double RELEASE_ASCENDING_THRESHOLD = -0.01;
    private static final double HOOP_EXIT_MARGIN = 0.01;
    private static final int MIN_RELEASE_FRAME = 2;

    // =========================
    // SMOOTH TRAJECTORY
    // =========================
    public List<BallPointDTO> smoothTrajectory(List<BallPointDTO> points) {

        if (points == null || points.size() < 3) {
            LOG.warn("Not enough points to smooth trajectory");
            return safeBallPoints(points);
        }

        List<BallPointDTO> result = new ArrayList<>();
        result.add(points.get(0));

        for (int i = 1; i < points.size() - 1; i++) {

            BallPointDTO p0 = points.get(i - 1);
            BallPointDTO p1 = points.get(i);
            BallPointDTO p2 = points.get(i + 1);

            double x = (p0.getX() + p1.getX() + p2.getX()) / 3.0;
            double y = (p0.getY() + p1.getY() + p2.getY()) / 3.0;

            result.add(new BallPointDTO((float) x, (float) y, p1.getFrame()));
        }

        result.add(points.get(points.size() - 1));

        LOG.infof("Trajectory smoothed -> input=%d output=%d",
                points.size(), result.size());

        return result;
    }

    // =========================
    // FILTER TRAJECTORY
    // =========================
    public List<BallPointDTO> filterTrajectory(List<BallPointDTO> points) {

        if (points == null || points.size() < 3) {
            LOG.warn("Not enough points to filter trajectory");
            return safeBallPoints(points);
        }

        List<BallPointDTO> filtered = new ArrayList<>();
        BallPointDTO prev = null;

        for (BallPointDTO p : points) {

            if (prev == null) {
                filtered.add(p);
                prev = p;
                continue;
            }

            double dx = Math.abs(p.getX() - prev.getX());
            double dy = Math.abs(p.getY() - prev.getY());

            double distance = euclideanDistance(
                    prev.getX(), prev.getY(),
                    p.getX(), p.getY()
            );

            if (distance > MAX_POINT_JUMP) {
                LOG.infof("Discard jump frame=%d dist=%.3f", p.getFrame(), distance);
                continue;
            }

            if (dx < DUPLICATE_THRESHOLD && dy < DUPLICATE_THRESHOLD) {
                LOG.debugf("Discard duplicate frame=%d", p.getFrame());
                continue;
            }

            filtered.add(p);
            prev = p;
        }

        LOG.infof("Trajectory filtered -> input=%d output=%d",
                points.size(), filtered.size());

        return filtered;
    }

    // =========================
    // STABILIZE RELEASE FRAME
    // =========================
    public int stabilizeReleaseFrame(int releaseFrame, List<BallPointDTO> trajectory) {

        if (trajectory == null || trajectory.size() < 3) {
            LOG.warn("Not enough trajectory points");
            return 0;
        }

        int f = clampReleaseFrame(releaseFrame, trajectory.size());

        BallPointDTO p0 = trajectory.get(f - 1);
        BallPointDTO p1 = trajectory.get(f);

        double dy = p1.getY() - p0.getY();

        if (dy < RELEASE_ASCENDING_THRESHOLD && f > MIN_RELEASE_FRAME) {
            LOG.infof("Release adjusted: %d -> %d", f, f - 1);
            f--;
        }

        return f;
    }

    public List<Point> extractFlightArc(ShotContext ctx) {

        if (ctx == null || ctx.ballNorm == null || ctx.ballNorm.size() < 3) {
            LOG.warn("Not enough ball points");
            return List.of();
        }

        if (ctx.hoopNorm == null) {
            LOG.warn("Hoop not available");
            return List.of();
        }

        final List<Point> trajectory = toPoints(ctx.ballNorm);

        final int releaseIndex = clampReleaseFrame(ctx.releaseFrame, trajectory.size());
        final int apexIndex = findApexIndex(trajectory, releaseIndex);

        final double hoopY = ctx.hoopNorm.getCenter().getY();

        List<Point> arc = new ArrayList<>();

        for (int i = releaseIndex; i < trajectory.size(); i++) {
            Point p = trajectory.get(i);
            arc.add(p);

            if (isAfterHoop(i, apexIndex, p, hoopY)) {
                break;
            }
        }

        LOG.infof("Arc extracted -> release=%d apex=%d size=%d",
                releaseIndex, apexIndex, arc.size());

        return arc;
    }

    private List<Point> toPoints(List<BallPointDTO> ballNorm) {
        return ballNorm.stream()
                .map(p -> new Point(p.getX(), p.getY()))
                .toList();
    }

    private int findApexIndex(List<Point> trajectory, int startIndex) {

        int apexIndex = startIndex;
        double minY = Double.MAX_VALUE;

        for (int i = startIndex; i < trajectory.size(); i++) {
            double y = trajectory.get(i).getY();

            if (y < minY) {
                minY = y;
                apexIndex = i;
            }
        }

        return apexIndex;
    }

    private boolean isAfterHoop(int i, int apexIndex, Point p, double hoopY) {
        return i > apexIndex && p.getY() > hoopY + HOOP_EXIT_MARGIN;
    }

    // =========================
    // FIT TRAJECTORY (FIXED 🔥)
    // =========================
    public PolynomialFunction fitTrajectory(ShotContext ctx) {

        List<Point> points = ctx.flightArcNorm;

        if (points == null || points.size() < 3) {
            LOG.warn("Not enough points to fit trajectory");
            return null;
        }

        try {
            WeightedObservedPoints obs = new WeightedObservedPoints();

            for (Point p : points) {
                obs.add(p.getX(), p.getY());
            }

            double[] coeff = PolynomialCurveFitter.create(2).fit(obs.toList());

            LOG.info("Fit coeff: " + Arrays.toString(coeff));

            // debug utile
            PolynomialFunction f = new PolynomialFunction(coeff);
            for (double x = 0; x <= 1.0; x += 0.25) {
                LOG.debugf("f(%.2f)=%.3f", x, f.value(x));
            }

            return f;

        } catch (Exception e) {
            LOG.error("Error fitting trajectory", e);
            return null;
        }
    }

    // =========================
    // HELPERS
    // =========================
    private int clampReleaseFrame(int releaseFrame, int size) {

        if (size < 3) return 0;

        if (releaseFrame < MIN_RELEASE_FRAME)
            return MIN_RELEASE_FRAME;

        if (releaseFrame >= size)
            return size - 1;

        return releaseFrame;
    }

    private int findApexIndex(List<Point> trajectory) {

        double minY = Double.MAX_VALUE;
        int index = 0;

        for (int i = 0; i < trajectory.size(); i++) {

            if (trajectory.get(i).getY() < minY) {
                minY = trajectory.get(i).getY();
                index = i;
            }
        }

        return index;
    }

    private double euclideanDistance(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private List<BallPointDTO> safeBallPoints(List<BallPointDTO> points) {
        return points == null ? Collections.emptyList() : points;
    }

    public PolynomialFunction buildPhysicsArc(ShotContext ctx, double apexHeight) {

        Point p0 = ctx.releaseNorm;

        if (ctx.hoopNorm == null || p0 == null) {
            LOG.warn("Missing points");
            return null;
        }

        // NORMALIZZA HOOP
        Point raw = ctx.hoopNorm.getCenter();
        Point p1 = new Point(
                raw.getX() / ctx.frameWidth,
                raw.getY() / ctx.frameHeight
        );

        double x0 = p0.getX();
        double y0 = p0.getY();

        double x1 = p1.getX();
        double y1 = p1.getY();

        if (Math.abs(x1 - x0) < 1e-6) {
            LOG.warn("Vertical shot not supported");
            return null;
        }

        // Apex
        double xm = (x0 + x1) / 2.0;

        double distance = Math.abs(x1 - x0);
        double ym = Math.min(y0, y1) - distance * apexHeight;

        ym = Math.max(0.0, ym);

        double[][] A = {
                {x0 * x0, x0, 1},
                {xm * xm, xm, 1},
                {x1 * x1, x1, 1}
        };

        double[] B = {y0, ym, y1};

        double[] coeff = solve3x3(A, B);

        LOG.infof("x0=%.3f x1=%.3f xm=%.3f", x0, x1, xm);

        return coeff != null ? new PolynomialFunction(coeff) : null;
    }

    private double[] solve3x3(double[][] A, double[] B) {

        double a = A[0][0], b = A[0][1], c = A[0][2];
        double d = A[1][0], e = A[1][1], f = A[1][2];
        double g = A[2][0], h = A[2][1], i = A[2][2];

        double det = a*(e*i - f*h) - b*(d*i - f*g) + c*(d*h - e*g);

        if (Math.abs(det) < 1e-9) return null;

        double dx = B[0]*(e*i - f*h) - b*(B[1]*i - f*B[2]) + c*(B[1]*h - e*B[2]);
        double dy = a*(B[1]*i - f*B[2]) - B[0]*(d*i - f*g) + c*(d*B[2] - B[1]*g);
        double dz = a*(e*B[2] - B[1]*h) - b*(d*B[2] - B[1]*g) + B[0]*(d*h - e*g);

        return new double[]{dx / det, dy / det, dz / det};
    }
}

