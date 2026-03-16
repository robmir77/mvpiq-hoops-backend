package com.mvpiq.service.ia;

import ai.djl.modality.cv.output.Point;
import com.mvpiq.dto.KeyPointDTO;
import com.mvpiq.dto.PoseFrameDTO;
import com.mvpiq.dto.ShotMetricsDTO;
import com.mvpiq.enums.HandSide;
import jakarta.inject.Inject;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;

import jakarta.enterprise.context.ApplicationScoped;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

@ApplicationScoped
public class OverlayDrawerService {

    @Inject
    PoseTrackingService poseTrackingService;

    public void drawOverlay(
            BufferedImage img,
            List<Point> ballPoints,
            PolynomialFunction realArc,
            PolynomialFunction idealArc,
            Point release,
            Point hoopCenter,
            double rimRadius,
            PoseFrameDTO pose,
            ShotMetricsDTO metrics) {

        if (img == null) {
            return;
        }

        Graphics2D g = img.createGraphics();

        g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int height = img.getHeight();

        // -------------------------
        // BALL
        // -------------------------

        drawBallPoints(g, ballPoints);
        drawArc(g, realArc, ballPoints, height, Color.GREEN);
        drawArc(g, idealArc, ballPoints, height, Color.BLUE);

        if (release != null) {
            drawRelease(g, release);
        }

        // -------------------------
        // HOOP
        // -------------------------

        if (hoopCenter != null) {
            drawHoop(g, hoopCenter, rimRadius);
        }

        // -------------------------
        // POSE
        // -------------------------

        if (pose != null) {

            HandSide shootingHand = HandSide.UNKNOWN;

            if (release != null) {
                shootingHand = poseTrackingService.detectShootingHand(
                        new com.mvpiq.dto.BallPointDTO(release.getX(), release.getY()),
                        pose);
            }

            drawPose(g, pose, shootingHand);
            drawJointAngles(g, pose, metrics, shootingHand);
        }

        g.dispose();
    }

    private void drawArc(Graphics2D g,
                         PolynomialFunction arc,
                         List<Point> trajectory,
                         int height,
                         Color color) {

        if (arc == null || trajectory == null || trajectory.size() < 2) return;

        g.setColor(color);

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;

        for (Point p : trajectory) {

            int x = (int) p.getX();

            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
        }

        int prevX = minX;
        int prevY = (int) arc.value(minX);

        for (int x = minX + 1; x <= maxX; x++) {

            int y = (int) arc.value(x);

            if (y >= 0 && y < height) {
                g.drawLine(prevX, prevY, x, y);
            }

            prevX = x;
            prevY = y;
        }
    }

    private void drawBallPoints(Graphics2D g, List<Point> points) {

        if (points == null) return;

        g.setColor(Color.RED);

        for (Point p : points) {
            g.fillOval((int)p.getX()-3,(int)p.getY()-3,6,6);
        }
    }

    private void drawRelease(Graphics2D g, Point release) {

        if (release == null) return;

        g.setColor(Color.YELLOW);
        g.fillOval((int)release.getX()-6,(int)release.getY()-6,12,12);
    }

    private void drawPose(
            Graphics2D g,
            PoseFrameDTO pose,
            HandSide shootingHand) {

        if (pose == null) {
            return;
        }

        g.setStroke(new BasicStroke(3));

        // BODY

        drawSegment(g, pose.getLeftShoulder(), pose.getRightShoulder(), Color.YELLOW);

        drawSegment(g, pose.getLeftShoulder(), pose.getLeftHip(), Color.YELLOW);
        drawSegment(g, pose.getRightShoulder(), pose.getRightHip(), Color.YELLOW);

        drawSegment(g, pose.getLeftHip(), pose.getRightHip(), Color.YELLOW);

        // LEGS

        drawSegment(g, pose.getLeftHip(), pose.getLeftKnee(), Color.YELLOW);
        drawSegment(g, pose.getLeftKnee(), pose.getLeftAnkle(), Color.YELLOW);

        drawSegment(g, pose.getRightHip(), pose.getRightKnee(), Color.YELLOW);
        drawSegment(g, pose.getRightKnee(), pose.getRightAnkle(), Color.YELLOW);

        // ARMS

        if (shootingHand == HandSide.RIGHT) {

            drawSegment(g, pose.getRightShoulder(), pose.getRightElbow(), Color.RED);
            drawSegment(g, pose.getRightElbow(), pose.getRightWrist(), Color.RED);

            drawSegment(g, pose.getLeftShoulder(), pose.getLeftElbow(), Color.YELLOW);
            drawSegment(g, pose.getLeftElbow(), pose.getLeftWrist(), Color.YELLOW);

        } else {

            drawSegment(g, pose.getLeftShoulder(), pose.getLeftElbow(), Color.RED);
            drawSegment(g, pose.getLeftElbow(), pose.getLeftWrist(), Color.RED);

            drawSegment(g, pose.getRightShoulder(), pose.getRightElbow(), Color.YELLOW);
            drawSegment(g, pose.getRightElbow(), pose.getRightWrist(), Color.YELLOW);
        }

        // KEYPOINTS

        drawKeypoint(g, pose.getLeftShoulder(), Color.CYAN);
        drawKeypoint(g, pose.getRightShoulder(), Color.CYAN);

        drawKeypoint(g, pose.getLeftElbow(), Color.CYAN);
        drawKeypoint(g, pose.getRightElbow(), Color.CYAN);

        drawKeypoint(g, pose.getLeftWrist(), Color.CYAN);
        drawKeypoint(g, pose.getRightWrist(), Color.CYAN);

        drawKeypoint(g, pose.getLeftHip(), Color.CYAN);
        drawKeypoint(g, pose.getRightHip(), Color.CYAN);

        drawKeypoint(g, pose.getLeftKnee(), Color.CYAN);
        drawKeypoint(g, pose.getRightKnee(), Color.CYAN);

        drawKeypoint(g, pose.getLeftAnkle(), Color.CYAN);
        drawKeypoint(g, pose.getRightAnkle(), Color.CYAN);
    }

    private void drawJointAngles(
            Graphics2D g,
            PoseFrameDTO pose,
            ShotMetricsDTO metrics,
            HandSide side) {

        if (metrics == null || pose == null) {
            return;
        }

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));

        KeyPointDTO elbow = side == HandSide.LEFT ? pose.getLeftElbow() : pose.getRightElbow();
        KeyPointDTO knee = side == HandSide.LEFT ? pose.getLeftKnee() : pose.getRightKnee();

        if (elbow != null && elbow.isValid(0.3)) {

            g.drawString(
                    "Elbow: " + Math.round(metrics.getElbowAngle()) + "°",
                    (int) elbow.getX() + 10,
                    (int) elbow.getY() - 10);
        }

        if (knee != null && knee.isValid(0.3)) {

            g.drawString(
                    "Knee: " + Math.round(metrics.getKneeAngle()) + "°",
                    (int) knee.getX() + 10,
                    (int) knee.getY() - 10);
        }
    }

    private void drawHoop(Graphics2D g, Point hoopCenter, double radius) {

        g.setColor(Color.ORANGE);
        g.setStroke(new BasicStroke(4));

        int x = (int) (hoopCenter.getX() - radius);
        int y = (int) (hoopCenter.getY() - radius);
        int d = (int) (radius * 2);

        g.drawOval(x, y, d, d);

        g.setColor(Color.RED);
        g.fillOval((int) hoopCenter.getX() - 4, (int) hoopCenter.getY() - 4, 8, 8);
    }

    private void drawKeypoint(Graphics2D g, KeyPointDTO p, Color color) {
        if (p == null || !p.isValid(0.3)) {
            return;
        }

        g.setColor(color);
        g.fillOval((int) p.getX() - 4, (int) p.getY() - 4, 8, 8);
    }

    private void drawSegment(Graphics2D g, KeyPointDTO a, KeyPointDTO b, Color color) {
        if (a == null || b == null || !a.isValid(0.3) || !b.isValid(0.3)) {
            return;
        }

        g.setColor(color);
        g.drawLine((int) a.getX(), (int) a.getY(), (int) b.getX(), (int) b.getY());
    }
}
