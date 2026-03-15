package com.mvpiq.service.ia;

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
}
