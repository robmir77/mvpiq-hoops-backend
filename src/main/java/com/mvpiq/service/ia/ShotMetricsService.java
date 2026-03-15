package com.mvpiq.service.ia;


import ai.djl.modality.cv.output.Point;
import com.mvpiq.dto.ShotMetricsDTO;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.awt.*;
import java.util.List;

import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;

@ApplicationScoped
public class ShotMetricsService {

    private static final Logger LOG = Logger.getLogger(ShotMetricsService.class.getName());

    // -------------------------
    // METRICS
    // -------------------------
    public ShotMetricsDTO calculateMetrics(List<Point> points,
                                           int hoopX,
                                           int hoopY,
                                           int rimRadius,
                                           double pixelToCm) {

        ShotMetricsDTO m = new ShotMetricsDTO();
        LOG.info("Calculating shot metrics");

        if (points == null || points.size() < 2) {
            LOG.warn("Not enough points for metrics calculation");
            return m;
        }

        // Fit parabola y = ax^2 + bx + c (X = orizzontale, Y = verticale)
        double[] coeff = fitParabola(points);
        double a = coeff[0];
        double b = coeff[1];
        double c = coeff[2];

        // RELEASE POINT
        Point release = findReleasePoint(points);
        LOG.infof("Release point -> x=%.2f y=%.2f", release.getX(), release.getY());

        // -------------------------
        // DISTANCE VERTICALE (laterale view)
        // -------------------------
        double distancePx = Math.abs(hoopY - release.getY());
        double distanceCm = distancePx * pixelToCm;
        LOG.infof("Shot distance -> %.2f cm", distanceCm);
        m.setDistance(distanceCm);

        // -------------------------
        // RELEASE ANGLE (in base alla parabola)
        // -------------------------
        double releaseSlope = 2 * a * release.getX() + b;
        m.setReleaseAngle(computeAngle(-releaseSlope));

        // -------------------------
        // ENTRY ANGLE (fine traiettoria)
        // -------------------------
        Point end = points.get(points.size() - 1);
        double entrySlope = 2 * a * end.getX() + b;
        m.setEntryAngle(computeAngle(-entrySlope));

        // -------------------------
        // ARC HEIGHT
        // -------------------------
        if (Math.abs(a) > 1e-6) {
            double xv = -b / (2 * a);
            double yv = a * xv * xv + b * xv + c;
            m.setArcHeight(release.getY() - yv);
        }

        // -------------------------
        // TRAJECTORY QUALITY
        // -------------------------
        double err = trajectoryError(points, a, b, c);
        m.setTrajectoryQuality(Math.max(0, 1 - err / 80));

        // -------------------------
        // RELEASE SPEED (vertical component)
        // -------------------------
        int idx = points.indexOf(release);
        if (idx < points.size() - 1) {
            Point next = points.get(idx + 1);
            double vy = next.getY() - release.getY();
            double speedCmFrame = vy * pixelToCm;
            m.setReleaseSpeed(speedCmFrame);
            LOG.infof("Release speed -> %.2f cm/frame", speedCmFrame);
        }

        // -------------------------
        // SHOT PREDICTION
        // -------------------------
        boolean make = willBallEnterHoop(a, b, c, hoopY, rimRadius);
        m.setMake(make);
        m.setMissType(shotMissType(a, b, c, hoopY, rimRadius));

        return m;
    }

    // -------------------------
    // UTILS
    // -------------------------
    private double computeAngle(double slope){
        return Math.toDegrees(Math.atan(slope));
    }

    // -------------------------
    // PARABOLA FIT
    // -------------------------
    private double[] fitParabola(List<Point> points) {

        int n = points.size();

        double sumX=0,sumX2=0,sumX3=0,sumX4=0;
        double sumY=0,sumXY=0,sumX2Y=0;

        for (Point p: points){
            double x = p.getX();
            double y = p.getY();
            double x2 = x*x;

            sumX += x;
            sumX2 += x2;
            sumX3 += x2*x;
            sumX4 += x2*x2;

            sumY += y;
            sumXY += x*y;
            sumX2Y += x2*y;
        }

        double[][] A = {{sumX4, sumX3, sumX2}, {sumX3, sumX2, sumX}, {sumX2, sumX, n}};
        double[] B = {sumX2Y, sumXY, sumY};

        return solve3x3(A, B);
    }

    private double[] solve3x3(double[][] A, double[] B){
        double a=A[0][0], b=A[0][1], c=A[0][2];
        double d=A[1][0], e=A[1][1], f=A[1][2];
        double g=A[2][0], h=A[2][1], i=A[2][2];

        double det=a*(e*i-f*h)-b*(d*i-f*g)+c*(d*h-e*g);
        if(Math.abs(det)<1e-6) return new double[]{0,0,0};

        double A1=(B[0]*(e*i-f*h)-b*(B[1]*i-f*B[2])+c*(B[1]*h-e*B[2]))/det;
        double B1=(a*(B[1]*i-f*B[2])-B[0]*(d*i-f*g)+c*(d*B[2]-B[1]*g))/det;
        double C1=(a*(e*B[2]-B[1]*h)-b*(d*B[2]-B[1]*g)+B[0]*(d*h-e*g))/det;

        return new double[]{A1,B1,C1};
    }

    // -------------------------
    // RELEASE POINT
    // -------------------------
    private Point findReleasePoint(List<Point> points) {
        if (points.size() < 3) return points.get(0);

        for (int i = 1; i < points.size() - 1; i++) {
            Point prev = points.get(i - 1);
            Point curr = points.get(i);
            Point next = points.get(i + 1);

            double dy1 = curr.getY() - prev.getY();
            double dy2 = next.getY() - curr.getY();

            if (dy1 < 0 && dy2 < 0) {
                return curr;
            }
        }

        return points.get(0);
    }

    // -------------------------
    // SHOT PREDICTION / MISS TYPE (laterale)
    // -------------------------
    private boolean willBallEnterHoop(double a,double b,double c,int hoopY,int rimRadius){
        double yBall = a*hoopY*hoopY + b*hoopY + c;
        return Math.abs(yBall - hoopY) < rimRadius * 0.9;
    }

    private String shotMissType(double a,double b,double c,int hoopY,int rimRadius){
        double yBall = a*hoopY*hoopY + b*hoopY + c;
        if(yBall > hoopY + rimRadius) return "short";
        if(yBall < hoopY - rimRadius) return "long";
        return "center";
    }

    private double trajectoryError(List<Point> points,double a,double b,double c){
        double error = 0;
        for(Point p: points){
            double predicted = a*p.getX()*p.getX() + b*p.getX() + c;
            error += Math.abs(predicted - p.getY());
        }
        return error / points.size();
    }

    public ShotMetricsDTO calculateMetricsVerticalOnly(
            List<Point> points,
            double verticalDistance,
            double pixelToCm,
            double hoopY,
            double rimRadius) {

        ShotMetricsDTO m = new ShotMetricsDTO();

        if (points == null || points.size() < 3) {
            return m;
        }

        // -------------------------
        // Fit parabola (trajectory smoothing)
        // -------------------------
        PolynomialFunction parabola = fitTrajectory(points);

        double a = parabola.getCoefficients()[2];
        double b = parabola.getCoefficients()[1];
        double c = parabola.getCoefficients()[0];

        // -------------------------
        // Release point
        // -------------------------
        Point release = findReleasePoint(points);

        m.setReleaseHeight(release.getY() * pixelToCm);

        // -------------------------
        // Distance (vertical shot distance)
        // -------------------------
        m.setDistance(verticalDistance);

        // -------------------------
        // RELEASE ANGLE
        // slope parabola nel punto di rilascio
        // -------------------------
        double releaseSlope = 2 * a * release.getX() + b;

        m.setReleaseAngle(Math.toDegrees(Math.atan(-releaseSlope)));

        // -------------------------
        // ENTRY ANGLE (al ferro)
        // -------------------------
        double entrySlope = 2 * a * points.get(points.size() - 1).getX() + b;

        m.setEntryAngle(Math.toDegrees(Math.atan(-entrySlope)));

        // -------------------------
        // ARC HEIGHT (apex della parabola)
        // -------------------------
        if (Math.abs(a) > 1e-6) {

            double apexX = -b / (2 * a);
            double apexY = a * apexX * apexX + b * apexX + c;

            double arcHeight = (release.getY() - apexY) * pixelToCm;

            m.setArcHeight(Math.abs(arcHeight));
        }

        // -------------------------
        // TRAJECTORY QUALITY
        // -------------------------
        double err = trajectoryError(points, a, b, c);

        m.setTrajectoryQuality(Math.max(0, 1 - err / 80));

        // -------------------------
        // RELEASE SPEED (vertical component)
        // -------------------------
        int idx = points.indexOf(release);

        if (idx < points.size() - 1) {

            Point next = points.get(idx + 1);

            double vy = next.getY() - release.getY();

            m.setReleaseSpeed(vy * pixelToCm);
        }

        // -------------------------
        // SHOT RESULT
        // -------------------------
        Point end = points.get(points.size() - 1);

        double finalY = end.getY();

        m.setMake(Math.abs(finalY - hoopY) <= rimRadius);

        if (finalY > hoopY + rimRadius) {
            m.setMissType("short");
        } else if (finalY < hoopY - rimRadius) {
            m.setMissType("long");
        } else {
            m.setMissType("center");
        }

        return m;
    }

    // Viene scartato il palleggio
    public int detectReleaseFrame(List<Point> trajectory) {

        if (trajectory == null || trajectory.size() < 8) {
            return 0;
        }

        int n = trajectory.size();

        // ignoriamo la prima parte (palleggio)
        int start = n / 3;

        int bestIndex = start;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (int i = start; i < n - 4; i++) {

            Point p0 = trajectory.get(i - 2);
            Point p1 = trajectory.get(i - 1);
            Point p2 = trajectory.get(i);
            Point p3 = trajectory.get(i + 1);
            Point p4 = trajectory.get(i + 2);
            Point p5 = trajectory.get(i + 3);

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

        return Math.max(0, bestIndex + 3);
    }

    private double distance(Point a, Point b){
        double dx = a.getX()-b.getX();
        double dy = a.getY()-b.getY();
        return Math.sqrt(dx*dx + dy*dy);
    }

    public double estimateShotSpeed(List<Point> trajectoryCm, int releaseFrame, int fps) {

        if (trajectoryCm == null || trajectoryCm.size() < releaseFrame + 2) {
            return 0;
        }

        // -------------------------
        // Find apex (highest point)
        // -------------------------
        int apexIndex = releaseFrame;

        double minY = trajectoryCm.get(releaseFrame).getY();

        for (int i = releaseFrame; i < trajectoryCm.size(); i++) {

            double y = trajectoryCm.get(i).getY();

            if (y < minY) {
                minY = y;
                apexIndex = i;
            }
        }

        if (apexIndex <= releaseFrame) {
            apexIndex = Math.min(releaseFrame + 3, trajectoryCm.size() - 1);
        }

        // -------------------------
        // Distance between release and apex
        // -------------------------
        double distance = 0;

        for (int i = releaseFrame + 1; i <= apexIndex; i++) {

            Point prev = trajectoryCm.get(i - 1);
            Point curr = trajectoryCm.get(i);

            double dx = curr.getX() - prev.getX();
            double dy = curr.getY() - prev.getY();

            distance += Math.sqrt(dx * dx + dy * dy);
        }

        // -------------------------
        // Time
        // -------------------------
        double timeSeconds = (apexIndex - releaseFrame) / (double) fps;

        if (timeSeconds == 0) {
            return 0;
        }

        // -------------------------
        // Speed
        // -------------------------
        double speedMs = (distance / 100.0) / timeSeconds;

        return speedMs * 3.6;
    }

    public double calculateShotDifficulty(ShotMetricsDTO m) {

        double distance = m.getTrajectoryDistance() / 100.0; // cm → m
        double speed = m.getShotSpeed();
        double arc = m.getArcHeight();

        // normalizzazioni

        double distanceScore = Math.min(distance / 14.0, 1.0); // metà campo = max
        double speedScore = Math.min(speed / 50.0, 1.0);       // 50 km/h max
        double arcScore = Math.min(arc / 300.0, 1.0);          // 3m arco alto

        double difficulty =
                distanceScore * 0.6 +
                        speedScore * 0.2 +
                        arcScore * 0.2;

        return difficulty * 100;
    }

    public PolynomialFunction fitTrajectory(List<Point> trajectory) {

        WeightedObservedPoints obs = new WeightedObservedPoints();

        for (Point p : trajectory) {
            obs.add(p.getX(), p.getY());
        }

        PolynomialCurveFitter fitter = PolynomialCurveFitter.create(2);

        double[] coeff = fitter.fit(obs.toList());

        return new PolynomialFunction(coeff);
    }

    public double computeApexHeight(List<Point> trajectory) {

        PolynomialFunction f = fitTrajectory(trajectory);

        double a = f.getCoefficients()[2];
        double b = f.getCoefficients()[1];

        double apexX = -b / (2 * a);

        return f.value(apexX);
    }

    public PolynomialFunction buildIdealArc(
            Point release,
            Point hoop,
            double arcHeightCm) {

        if (release == null || hoop == null) {
            return null;
        }

        // -------------------------
        // Vertice parabola ideale
        // -------------------------
        double xv = (release.getX() + hoop.getX()) / 2.0;

        // apex sopra il ferro
        double yv = hoop.getY() - arcHeightCm;

        // -------------------------
        // Calcolo coefficiente a
        // usando il punto di rilascio
        // -------------------------
        double xr = release.getX();
        double yr = release.getY();

        double a = (yr - yv) / ((xr - xv) * (xr - xv));

        // conversione forma standard
        double b = -2 * a * xv;
        double c = a * xv * xv + yv;

        return new PolynomialFunction(new double[]{c, b, a});
    }

    public void drawArc(
            Graphics2D g,
            PolynomialFunction arc,
            int width,
            Color color) {

        if (arc == null) return;

        g.setColor(color);

        for (int x = 0; x < width; x++) {

            double y = arc.value(x);

            if (y >= 0 && y < 2000) {
                g.fillOval(x, (int) y, 3, 3);
            }
        }
    }

    public double trajectoryDeviation(
            List<Point> trajectory,
            PolynomialFunction idealArc){

        double error = 0;

        for(Point p : trajectory){

            double idealY = idealArc.value(p.getX());

            error += Math.abs(p.getY() - idealY);
        }

        return error / trajectory.size();
    }
}