package com.mvpiq.service.ia;

import ai.djl.modality.cv.output.Point;
import com.mvpiq.dto.BallPointDTO;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class TrajectoryService {

    private static final Logger LOG = Logger.getLogger(TrajectoryService.class);

    // -------------------------
    // SMOOTH TRAJECTORY
    // -------------------------

    List<BallPointDTO> smoothTrajectory(List<BallPointDTO> points) {

        if (points.size() < 3) return points;

        List<BallPointDTO> result = new ArrayList<>();

        result.add(points.get(0));

        for (int i = 1; i < points.size() - 1; i++) {

            BallPointDTO p0 = points.get(i - 1);
            BallPointDTO p1 = points.get(i);
            BallPointDTO p2 = points.get(i + 1);

            double x = (p0.getX() + p1.getX() + p2.getX()) / 3;
            double y = (p0.getY() + p1.getY() + p2.getY()) / 3;

            result.add(new BallPointDTO((float) x, (float) y, i));
        }

        result.add(points.get(points.size() - 1));

        return result;
    }

    public List<BallPointDTO> filterTrajectory(List<BallPointDTO> points) {

        if (points == null || points.size() < 3) {
            return points;
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

            double distance = Math.sqrt(dx * dx + dy * dy);

            // salto impossibile → scarta
            if (distance > 150) {
                continue;
            }

            // punto duplicato → scarta
            if (dx < 1 && dy < 1) {
                continue;
            }

            filtered.add(p);
            prev = p;
        }

        return filtered;
    }

    public int stabilizeReleaseFrame(int releaseFrame, List<Point> trajectory) {

        if (trajectory == null || trajectory.size() < 3) {
            return 0;
        }

        // evita frame troppo precoci
        if (releaseFrame < 2) {
            releaseFrame = 2;
        }

        if (releaseFrame >= trajectory.size()) {
            releaseFrame = trajectory.size() - 1;
        }

        // controllo velocità iniziale
        Point p0 = trajectory.get(releaseFrame - 1);
        Point p1 = trajectory.get(releaseFrame);

        double dy = p1.getY() - p0.getY();

        // se la palla sta ancora salendo, probabilmente release è prima
        if (dy < -5 && releaseFrame > 2) {
            releaseFrame -= 1;
        }

        return releaseFrame;
    }

    public List<Point> extractShotArc(List<Point> trajectory, double hoopY) {

        if (trajectory == null || trajectory.size() < 3) {
            return trajectory;
        }

        List<Point> arc = new ArrayList<>();

        // trova apice
        double minY = Double.MAX_VALUE;
        int apexIndex = 0;

        for (int i = 0; i < trajectory.size(); i++) {

            Point p = trajectory.get(i);

            if (p.getY() < minY) {
                minY = p.getY();
                apexIndex = i;
            }
        }

        // prendi punti fino al ferro
        for (int i = 0; i < trajectory.size(); i++) {

            Point p = trajectory.get(i);

            arc.add(p);

            // fermati quando la palla scende sotto il ferro
            if (i > apexIndex && p.getY() > hoopY + 10) {
                break;
            }
        }

        return arc;
    }
}
