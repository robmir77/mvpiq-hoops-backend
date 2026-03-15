package com.mvpiq.service.ia;

import ai.djl.modality.cv.output.Point;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;

import jakarta.enterprise.context.ApplicationScoped;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

@ApplicationScoped
public class TrajectoryOverlayService {

    public void drawOverlay(
            BufferedImage img,
            List<Point> ballPoints,
            PolynomialFunction realArc,
            PolynomialFunction idealArc,
            Point release,
            Point hoop,
            double rimRadius) {

        if (ballPoints == null || ballPoints.size() < 2) {
            return;
        }

        Graphics2D g = img.createGraphics();

        g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int height = img.getHeight();

        drawBallPoints(g, ballPoints);
        drawArc(g, realArc, ballPoints, height, Color.GREEN);
        drawArc(g, idealArc, ballPoints, height, Color.BLUE);
        drawRelease(g, release);
        drawHoop(g, hoop, rimRadius);

        g.dispose();
    }

    private void drawArc(Graphics2D g,
                         PolynomialFunction arc,
                         List<Point> trajectory,
                         int height,
                         Color color) {

        if (arc == null || trajectory == null || trajectory.size() < 2) return;

        g.setColor(color);

        // -------------------------
        // trova minX e maxX
        // -------------------------
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

    private void drawHoop(Graphics2D g, Point hoop, double radius) {

        if (hoop == null) return;

        g.setColor(Color.ORANGE);

        g.drawOval(
                (int)(hoop.getX() - radius),
                (int)(hoop.getY() - radius),
                (int)(radius * 2),
                (int)(radius * 2)
        );
    }
}
