package com.mvpiq.service.ai;

import ai.djl.modality.cv.output.Point;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class TrajectoryService {

    // -------------------------
    // SMOOTH TRAJECTORY
    // -------------------------

    List<Point> smoothTrajectory(List<Point> points) {

        if (points.size() < 3) return points;

        List<Point> result = new ArrayList<>();

        result.add(points.get(0));

        for (int i = 1; i < points.size() - 1; i++) {

            Point p0 = points.get(i - 1);
            Point p1 = points.get(i);
            Point p2 = points.get(i + 1);

            double x = (p0.getX() + p1.getX() + p2.getX()) / 3;
            double y = (p0.getY() + p1.getY() + p2.getY()) / 3;

            result.add(new Point((float) x, (float) y));
        }

        result.add(points.get(points.size() - 1));

        return result;
    }
}
