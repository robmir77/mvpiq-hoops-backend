package com.mvpiq.service.ia;

import ai.djl.modality.cv.output.Point;
import com.mvpiq.dto.BallPointDTO;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class TrajectoryService {

    private static final Logger LOG = Logger.getLogger(TrajectoryService.class);

    private static final double MAX_POINT_JUMP_PX = 150.0;
    private static final double DUPLICATE_THRESHOLD_PX = 1.0;
    private static final double RELEASE_ASCENDING_THRESHOLD = -5.0;
    private static final double HOOP_EXIT_MARGIN_PX = 10.0;
    private static final int MIN_RELEASE_FRAME = 2;

    // -------------------------
    // SMOOTH TRAJECTORY
    // -------------------------
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

    // -------------------------
    // FILTER TRAJECTORY
    // -------------------------
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

            // salto impossibile → scarta
            if (distance > MAX_POINT_JUMP_PX) {
                LOG.debugf("Discarding point at frame %d: impossible jump detected (distance=%.2f)",
                        Optional.of(p.getFrame()), distance);
                continue;
            }

            // punto duplicato → scarta
            if (dx < DUPLICATE_THRESHOLD_PX && dy < DUPLICATE_THRESHOLD_PX) {
                LOG.debugf("Discarding point at frame %d: duplicate point detected",
                        p.getFrame());
                continue;
            }

            filtered.add(p);
            prev = p;
        }

        LOG.infof("Trajectory filtered -> input=%d output=%d",
                points.size(), filtered.size());

        return filtered;
    }

    // -------------------------
    // STABILIZE RELEASE FRAME
    // -------------------------
    public int stabilizeReleaseFrame(int releaseFrame, List<Point> trajectory) {

        if (trajectory == null || trajectory.size() < 3) {
            LOG.warn("Not enough trajectory points to stabilize release frame");
            return 0;
        }

        int stabilizedFrame = clampReleaseFrame(releaseFrame, trajectory.size());

        Point p0 = trajectory.get(stabilizedFrame - 1);
        Point p1 = trajectory.get(stabilizedFrame);

        double dy = p1.getY() - p0.getY();

        // se la palla sta ancora salendo, probabilmente release è prima
        if (dy < RELEASE_ASCENDING_THRESHOLD && stabilizedFrame > MIN_RELEASE_FRAME) {
            LOG.infof("Release frame adjusted backward -> from=%d to=%d",
                    stabilizedFrame, stabilizedFrame - 1);
            stabilizedFrame -= 1;
        }

        LOG.infof("Release frame stabilized -> %d", stabilizedFrame);

        return stabilizedFrame;
    }

    // -------------------------
    // EXTRACT SHOT ARC
    // -------------------------
    public List<Point> extractShotArc(List<Point> trajectory, double hoopY) {

        if (trajectory == null || trajectory.size() < 3) {
            LOG.warn("Not enough points to extract shot arc");
            return safePoints(trajectory);
        }

        List<Point> arc = new ArrayList<>();

        // trova apice
        int apexIndex = findApexIndex(trajectory);

        // prendi punti fino al ferro
        for (int i = 0; i < trajectory.size(); i++) {

            Point p = trajectory.get(i);

            arc.add(p);

            // fermati quando la palla scende sotto il ferro
            if (i > apexIndex && p.getY() > hoopY + HOOP_EXIT_MARGIN_PX) {
                break;
            }
        }

        LOG.infof("Shot arc extracted -> apexIndex=%d points=%d",
                apexIndex, arc.size());

        return arc;
    }

    // -------------------------
    // PRIVATE HELPERS
    // -------------------------
    private int clampReleaseFrame(int releaseFrame, int trajectorySize) {

        if (trajectorySize < 3) {
            return 0;
        }

        if (releaseFrame < MIN_RELEASE_FRAME) {
            return MIN_RELEASE_FRAME;
        }

        if (releaseFrame >= trajectorySize) {
            return trajectorySize - 1;
        }

        return releaseFrame;
    }

    private int findApexIndex(List<Point> trajectory) {

        double minY = Double.MAX_VALUE;
        int apexIndex = 0;

        for (int i = 0; i < trajectory.size(); i++) {

            Point p = trajectory.get(i);

            if (p.getY() < minY) {
                minY = p.getY();
                apexIndex = i;
            }
        }

        return apexIndex;
    }

    private double euclideanDistance(double x1, double y1, double x2, double y2) {

        double dx = x2 - x1;
        double dy = y2 - y1;

        return Math.sqrt(dx * dx + dy * dy);
    }

    private List<BallPointDTO> safeBallPoints(List<BallPointDTO> points) {
        return points == null ? Collections.emptyList() : points;
    }

    private List<Point> safePoints(List<Point> points) {
        return points == null ? Collections.emptyList() : points;
    }

    public List<Point> extractFlightArc(
            List<Point> trajectory,
            int releaseFrame,
            double hoopY) {

        if (trajectory == null || trajectory.size() < 3) {
            LOG.warn("Not enough points to extract flight arc");
            return safePoints(trajectory);
        }

        if (releaseFrame < 0 || releaseFrame >= trajectory.size()) {
            LOG.warnf("Invalid releaseFrame %d for trajectory size %d",
                    releaseFrame, trajectory.size());
            releaseFrame = 0;
        }

        List<Point> arc = new ArrayList<>();

        // trova apice dopo il rilascio
        int apexIndex = findApexIndex(
                trajectory.subList(releaseFrame, trajectory.size())
        ) + releaseFrame;

        for (int i = releaseFrame; i < trajectory.size(); i++) {

            Point p = trajectory.get(i);
            arc.add(p);

            // quando la palla scende sotto il ferro fermiamo l'arco
            if (i > apexIndex && p.getY() > hoopY + HOOP_EXIT_MARGIN_PX) {
                break;
            }
        }

        LOG.infof(
                "Flight arc extracted -> release=%d apex=%d points=%d",
                releaseFrame,
                apexIndex,
                arc.size()
        );

        return arc;
    }
}
