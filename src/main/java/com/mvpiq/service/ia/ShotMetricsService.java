package com.mvpiq.service.ia;


import ai.djl.modality.cv.output.Point;
import com.mvpiq.dto.ShotMetrics;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class ShotMetricsService {

    private static final Logger LOG = Logger.getLogger(ShotMetricsService.class.getName());

    // -------------------------
    // METRICS
    // -------------------------
    public ShotMetrics calculateMetrics(List<Point> points,
                                        int hoopX,
                                        int hoopY,
                                        int rimRadius,
                                        double pixelToCm) {

        ShotMetrics m = new ShotMetrics();
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

    public ShotMetrics calculateMetricsVerticalOnly(List<Point> points, double verticalDistance, double pixelToCm, double hoopY, double rimRadius) {
        ShotMetrics m = new ShotMetrics();

        if (points == null || points.size() < 2) {
            return m; // troppo pochi punti
        }

        // RELEASE POINT
        Point release = findReleasePoint(points);
        m.setReleaseHeight(release.getY() * pixelToCm); // altezza reale in cm

        // DISTANCE VERTICALE
        m.setDistance(verticalDistance);

        // RELEASE SPEED (solo verticale)
        int idx = points.indexOf(release);
        if (idx < points.size() - 1) {
            Point next = points.get(idx + 1);
            double vy = next.getY() - release.getY();
            m.setReleaseSpeed(vy * pixelToCm);
        }

        // ENTRY ANGLE (pendenza verticale)
        Point end = points.get(points.size() - 1);
        double dy = end.getY() - release.getY();
        double dx = end.getX() - release.getX();
        if (dx != 0) {
            m.setEntryAngle(Math.toDegrees(Math.atan(dy / dx)));
        }

        // ARC HEIGHT
        double[] coeff = fitParabola(points);
        double a = coeff[0], b = coeff[1], c = coeff[2];
        if (Math.abs(a) > 1e-6) {
            double xv = -b / (2 * a);
            double yv = a * xv * xv + b * xv + c;
            m.setArcHeight(release.getY() - yv);
        }

        // Trajectory quality
        double err = trajectoryError(points, a, b, c);
        m.setTrajectoryQuality(Math.max(0, 1 - err / 80));

        // Shot result
        double finalY = end.getY(); // altezza palla all'arrivo
        m.setMake(Math.abs(finalY - hoopY) <= rimRadius);
        if (finalY > hoopY + rimRadius) m.setMissType("short");
        else if (finalY < hoopY - rimRadius) m.setMissType("long");
        else m.setMissType("center");

        return m;
    }
}