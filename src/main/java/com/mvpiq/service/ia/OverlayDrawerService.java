package com.mvpiq.service.ia;

import ai.djl.modality.cv.output.Point;
import com.mvpiq.dto.KeyPointDTO;
import com.mvpiq.dto.PoseFrameDTO;
import com.mvpiq.dto.ShotMetricsDTO;
import com.mvpiq.enums.HandSide;
import jakarta.inject.Inject;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

@ApplicationScoped
public class OverlayDrawerService {

    private static final Logger LOG = Logger.getLogger(OverlayDrawerService.class);

    @Inject
    PoseTrackingService poseTrackingService;

    public void drawOverlay(BufferedImage img, ShotContext ctx, int frameIndex) {

        if (img == null || ctx == null) return;

        Graphics2D g = img.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int width = img.getWidth();
        int height = img.getHeight();

        // =========================
        // BALL (NORMALIZED → PIXEL)
        // =========================
        if (ctx.ballNorm != null && !ctx.ballNorm.isEmpty()) {

            List<Point> partialPx = ctx.ballNorm.subList(
                            0,
                            Math.min(frameIndex + 1, ctx.ballNorm.size())
                    ).stream()
                    .map(p -> new Point(
                            p.getX() * width,
                            p.getY() * height
                    ))
                    .toList();

            drawBallPoints(g, partialPx);
        }

        // =========================
        // TRAJECTORY (NORMALIZED → PIXEL)
        // =========================
        drawArcNorm(g, ctx.realArcNorm, width, height, Color.BLUE);   // dati reali
        //drawArcNorm(g, ctx.physicsArcNorm, width, height, Color.RED);
        drawArcNorm(g, ctx.idealArcNorm, width, height, Color.GREEN); // ideale

        // =========================
        // RELEASE (NORMALIZED → PIXEL)
        // =========================
        if (ctx.releaseNorm != null) {
            Point releasePx = new Point(
                    ctx.releaseNorm.getX() * width,
                    ctx.releaseNorm.getY() * height
            );
            drawReleasePx(g, releasePx);
        }

        // =========================
        // HOOP (NORMALIZED → PIXEL)
        // =========================
        if (ctx.hoopNorm != null && ctx.hoopNorm.getCenter() != null) {

            Point hoopPx = new Point(
                    ctx.hoopNorm.getCenter().getX() * width,
                    ctx.hoopNorm.getCenter().getY() * height
            );

            double radiusPx = ctx.hoopNorm.radius * width;

            drawHoopPx(g, hoopPx, radiusPx);
        }

        // =========================
        // POSE (PIXEL)
        // =========================
        if (ctx.poseFrames != null && frameIndex < ctx.poseFrames.size()) {

            PoseFrameDTO pose = ctx.poseFrames.get(frameIndex);

            HandSide hand = ctx.shootingHand != null
                    ? ctx.shootingHand
                    : HandSide.UNKNOWN;

            drawPose(g, pose, hand);
            drawJointAngles(g, pose, ctx.metrics, hand);
        }

        g.dispose();
    }

    // =========================
    // DRAW HELPERS
    // =========================

    private void drawBallPoints(Graphics2D g, List<Point> pts) {
        g.setColor(Color.YELLOW);

        for (Point p : pts) {
            int x = (int) p.getX();
            int y = (int) p.getY();
            g.fillOval(x - 3, y - 3, 6, 6);
        }
    }

    private void drawArcNorm(Graphics2D g,
                             PolynomialFunction arc,
                             int width,
                             int height,
                             Color color) {

        if (arc == null) {
            LOG.warn("drawArcNorm -> arc is null");
            return;
        }

        double step = 1.0 / width;

        double minY = Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;

        double y0 = arc.value(0);

        if (Double.isNaN(y0) || Double.isInfinite(y0)) {
            LOG.warn("drawArcNorm -> invalid initial value");
            return;
        }

        int prevX = 0;
        int prevY = (int) (y0 * height);

        for (int i = 1; i <= width; i++) {

            double x = i * step;
            double y = arc.value(x);

            if (Double.isNaN(y) || Double.isInfinite(y)) {
                LOG.infof("Arc invalid at x=%.3f", x);
                continue;
            }

            // 🔍 track range
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;

            int px = i;
            int py = (int) (y * height);

            g.setColor(color);
            g.drawLine(prevX, prevY, px, py);

            prevX = px;
            prevY = py;
        }

        // =========================
        // DEBUG FINALE 🔥
        // =========================
        LOG.infof("Arc draw -> yRange=[%.3f, %.3f]", minY, maxY);

        double yMid = arc.value(0.5);
        LOG.infof("Arc sample -> f(0.5)=%.3f", yMid);
    }

    private double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private void drawReleasePx(Graphics2D g, Point p) {
        g.setColor(Color.RED);
        g.fillOval((int) p.getX() - 6, (int) p.getY() - 6, 12, 12);
    }

    private void drawHoopPx(Graphics2D g, Point center, double radius) {

        g.setColor(Color.ORANGE);

        int x = (int) center.getX();
        int y = (int) center.getY();
        int r = (int) radius;

        g.drawOval(x - r, y - r, r * 2, r * 2);
    }

    // =========================
    // POSE
    // =========================

    private void drawPose(Graphics2D g,
                          PoseFrameDTO pose,
                          HandSide shootingHand) {

        if (pose == null) return;

        g.setStroke(new BasicStroke(3));

        drawSegment(g, pose.getLeftShoulder(), pose.getRightShoulder(), Color.YELLOW);
        drawSegment(g, pose.getLeftShoulder(), pose.getLeftHip(), Color.YELLOW);
        drawSegment(g, pose.getRightShoulder(), pose.getRightHip(), Color.YELLOW);
        drawSegment(g, pose.getLeftHip(), pose.getRightHip(), Color.YELLOW);

        drawSegment(g, pose.getLeftHip(), pose.getLeftKnee(), Color.YELLOW);
        drawSegment(g, pose.getLeftKnee(), pose.getLeftAnkle(), Color.YELLOW);

        drawSegment(g, pose.getRightHip(), pose.getRightKnee(), Color.YELLOW);
        drawSegment(g, pose.getRightKnee(), pose.getRightAnkle(), Color.YELLOW);

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

        drawKeypoint(g, pose.getLeftShoulder(), Color.CYAN);
        drawKeypoint(g, pose.getRightShoulder(), Color.CYAN);
        drawKeypoint(g, pose.getLeftElbow(), Color.CYAN);
        drawKeypoint(g, pose.getRightElbow(), Color.CYAN);
        drawKeypoint(g, pose.getLeftWrist(), Color.CYAN);
        drawKeypoint(g, pose.getRightWrist(), Color.CYAN);
    }

    private void drawJointAngles(Graphics2D g,
                                 PoseFrameDTO pose,
                                 ShotMetricsDTO metrics,
                                 HandSide side) {

        if (metrics == null || pose == null) return;

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));

        KeyPointDTO elbow = side == HandSide.LEFT
                ? pose.getLeftElbow()
                : pose.getRightElbow();

        if (elbow != null && elbow.isValid(0.3)) {
            g.drawString(
                    "Elbow: " + Math.round(metrics.getElbowAngle()) + "°",
                    (int) elbow.getX() + 10,
                    (int) elbow.getY() - 10
            );
        }
    }

    private void drawKeypoint(Graphics2D g, KeyPointDTO p, Color color) {
        if (p == null || !p.isValid(0.3)) return;

        g.setColor(color);
        g.fillOval((int) p.getX() - 4, (int) p.getY() - 4, 8, 8);
    }

    private void drawSegment(Graphics2D g, KeyPointDTO a, KeyPointDTO b, Color color) {
        if (a == null || b == null || !a.isValid(0.3) || !b.isValid(0.3)) return;

        g.setColor(color);
        g.drawLine((int) a.getX(), (int) a.getY(), (int) b.getX(), (int) b.getY());
    }
}