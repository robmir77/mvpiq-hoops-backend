package com.mvpiq.service.ia;

import ai.djl.modality.cv.output.Point;
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

    public Hoop predictHoopFromTrajectory(List<Point> trajectoryPoints) {
        if (trajectoryPoints.size() < 3) {
            LOG.warn("Too few points to predict hoop");
            return null;
        }

        // Prendiamo gli ultimi 3 punti rilevati della palla
        Point p0 = trajectoryPoints.get(trajectoryPoints.size() - 3);
        Point p1 = trajectoryPoints.get(trajectoryPoints.size() - 2);
        Point p2 = trajectoryPoints.get(trajectoryPoints.size() - 1);

        // Fit parabola y = ax^2 + bx + c usando i 3 punti
        double x0 = p0.getX(), y0 = p0.getY();
        double x1 = p1.getX(), y1 = p1.getY();
        double x2 = p2.getX(), y2 = p2.getY();

        // Sistema lineare per trovare a, b, c
        // [x^2 x 1][a] = [y]
        double[][] mat = {
                {x0*x0, x0, 1},
                {x1*x1, x1, 1},
                {x2*x2, x2, 1}
        };
        double[] yVec = {y0, y1, y2};

        double[] coeffs = solveLinearSystem(mat, yVec); // restituisce {a, b, c}
        if (coeffs == null) {
            LOG.warn("Parabola fit failed");
            return null;
        }

        double a = coeffs[0], b = coeffs[1], c = coeffs[2];

        // Troviamo il vertice della parabola: x = -b/(2a)
        double hoopX = -b / (2 * a);
        double hoopY = a * hoopX * hoopX + b * hoopX + c;

        // Stima del raggio (media distanza tra punti successivi)
        double dx = Math.abs(p2.getX() - p0.getX());
        double dy = Math.abs(p2.getY() - p0.getY());
        int estimatedRadius = (int) Math.max(dx, dy) / 2;

        LOG.infof("Predicted hoop -> x=%.1f y=%.1f r=%d", hoopX, hoopY, estimatedRadius);

        return new Hoop((int) hoopX, (int) hoopY, estimatedRadius);
    }

    /**
     * Risolve un sistema lineare 3x3 con matrice non singolare
     */
    private double[] solveLinearSystem(double[][] m, double[] y) {
        try {
            double det = m[0][0]*(m[1][1]*m[2][2]-m[1][2]*m[2][1])
                    - m[0][1]*(m[1][0]*m[2][2]-m[1][2]*m[2][0])
                    + m[0][2]*(m[1][0]*m[2][1]-m[1][1]*m[2][0]);
            if (Math.abs(det) < 1e-6) return null;

            // Cramer’s rule
            double[] sol = new double[3];
            for (int i = 0; i < 3; i++) {
                double[][] tmp = new double[3][3];
                for (int r = 0; r < 3; r++) System.arraycopy(m[r], 0, tmp[r], 0, 3);
                for (int r = 0; r < 3; r++) tmp[r][i] = y[r];
                sol[i] = determinant3x3(tmp) / det;
            }
            return sol;
        } catch (Exception e) {
            LOG.error("Linear system solve failed", e);
            return null;
        }
    }

    private double determinant3x3(double[][] m) {
        return m[0][0]*(m[1][1]*m[2][2]-m[1][2]*m[2][1])
                - m[0][1]*(m[1][0]*m[2][2]-m[1][2]*m[2][0])
                + m[0][2]*(m[1][0]*m[2][1]-m[1][1]*m[2][0]);
    }
}
