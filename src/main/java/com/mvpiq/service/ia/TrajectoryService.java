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
            return null;
        }

        // === NORMALIZZAZIONE HOOP ===
        Point raw = ctx.hoopNorm.getCenter();
        Point p1 = new Point(
                raw.getX() / ctx.frameWidth,
                raw.getY() / ctx.frameHeight
        );

        double x0 = p0.getX();
        double y0 = p0.getY();

        double x1 = p1.getX();
        double y1 = p1.getY();

        // === DIREZIONE SEMPRE DA SINISTRA A DESTRA ===
        boolean flipped = false;
        if (x1 < x0) {
            flipped = true;

            double tmpX = x0; x0 = x1; x1 = tmpX;
            double tmpY = y0; y0 = y1; y1 = tmpY;
        }

        double dx = x1 - x0;
        if (Math.abs(dx) < 1e-6) {
            return null;
        }

        // === DISTANZA ===
        double distance = Math.abs(dx);

        // === APEX (spostato in avanti, NON al centro) ===
        double xm = x0 + dx * 0.45;

        // === ALTEZZA ARCO (scalata e limitata) ===
        double arcBoost = Math.min(distance, 0.5);
        double baseHeight = Math.min(y0, y1);

        double ym = baseHeight - (arcBoost * apexHeight);
        ym = Math.max(0.05, ym); // evita schiacciamenti

        // === COSTRUZIONE PARABOLA (forma stabile) ===
        // Forma: y = a(x - xm)^2 + ym

        double a = (y0 - ym) / ((x0 - xm) * (x0 - xm));

        // Se per qualche motivo è positiva → forziamo concavità corretta
        if (a > 0) {
            a = -Math.abs(a);
        }

        double b = -2 * a * xm;
        double c = a * xm * xm + ym;

        // === SE AVEVI FLIPPATO → RIPRISTINA ===
        if (flipped) {
            // riflessione orizzontale
            double newA = a;
            double newB = -b;
            double newC = c;

            return new PolynomialFunction(new double[]{newC, newB, newA});
        }

        return new PolynomialFunction(new double[]{c, b, a});
    }
}

