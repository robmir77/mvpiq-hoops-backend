package com.mvpiq.service.ia;

import ai.djl.modality.cv.output.Point;
import com.mvpiq.dto.BallPointDTO;
import com.mvpiq.dto.PoseFrameDTO;
import com.mvpiq.dto.ShotMetricsDTO;
import com.mvpiq.enums.HandSide;
import com.mvpiq.enums.ShotType;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.jboss.logging.Logger;

import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class ShotContext {

    // =========================
    // 🎯 SHOT CONFIG
    // =========================
    public ShotType shotType = ShotType.AUTO;
    public double shotDistanceMeters;

    // =========================
    // 🔥 SCALE
    // =========================
    public double metersPerPixel;
    public double hoopPixelDiameter;

    // =========================
    // 🎬 VIDEO
    // =========================
    public List<File> frames;
    public int fps;

    public int frameWidth;
    public int frameHeight;

    public List<AffineTransform> frameTransforms;
    public boolean stabilized;

    // =========================
    // 🏀 HOOP
    // =========================
    public Hoop hoopNorm;

    // =========================
    // 🏀 BALL
    // =========================
    public List<BallPointDTO> ballNorm;

    // =========================
    // 🧍 POSE
    // =========================
    public List<PoseFrameDTO> poseFrames;
    public HandSide shootingHand;

    // =========================
    // 🎯 EVENTS
    // =========================
    public int startShotFrame;
    public int releaseFrame;

    public Point releaseNorm;

    // =========================
    // 📈 TRAJECTORIES
    // =========================
    public List<Point> flightArcNorm;

    public PolynomialFunction realArcNorm;
    public PolynomialFunction idealArcNorm;
    public PolynomialFunction physicsArcNorm;

    // =========================
    // 📊 OUTPUT
    // =========================
    public ShotMetricsDTO metrics;

    public ShotContext(List<File> frames, int fps, ShotType shotType) {
        this.frames = frames;
        this.fps = fps;
        this.shotType = shotType;
    }

    // =========================
    // 🔧 SCALE COMPUTATION
    // =========================
    public void computeScale() {

        if (hoopPixelDiameter <= 0) {
            metersPerPixel = 0;
            return;
        }

        metersPerPixel = 0.45 / hoopPixelDiameter;
    }

    public void initScale(Logger log) {

        if (hoopNorm == null || frameWidth <= 0 || frameHeight <= 0) {
            log.warn("initScale -> missing data");
            return;
        }

        // 🔥 conversione precisa usando normToPixel
        Point center = normToPixel(new Point(
                hoopNorm.center.getX(),
                hoopNorm.center.getY()
        ));

        Point edge = normToPixel(new Point(
                hoopNorm.center.getX() + hoopNorm.getRadius(),
                hoopNorm.center.getY()
        ));

        double radiusPx = Math.abs(edge.getX() - center.getX());
        hoopPixelDiameter = radiusPx * 2.0;

        if (hoopPixelDiameter <= 0) {
            log.warn("initScale -> invalid hoop diameter");
            return;
        }

        metersPerPixel = 0.45 / hoopPixelDiameter;

        log.infof("Scale initialized -> hoopPx=%.2f metersPerPixel=%.5f",
                hoopPixelDiameter, metersPerPixel);
    }

    // =========================
    // 🎯 NORMALIZED → PIXEL
    // =========================
    public Point normToPixel(Point norm) {

        if (norm == null) return null;

        // 🔥 FIX REALE (non height/width!)
        double aspectRatio = (double) frameWidth / frameHeight;

        double px = norm.getX() * frameWidth;
        double py = norm.getY() * frameHeight * aspectRatio;

        return new Point(px, py);
    }

    public double normXToPixel(double x) {
        return x * frameWidth;
    }

    public double normYToPixel(double y) {
        double aspectRatio = (double) frameWidth / frameHeight;
        return y * frameHeight * aspectRatio;
    }

    public Point pixelToNorm(Point px) {

        if (px == null) return null;

        double aspectRatio = (double) frameWidth / frameHeight;

        double nx = px.getX() / frameWidth;
        double ny = px.getY() / (frameHeight * aspectRatio);

        return new Point(nx, ny);
    }

    // =========================
    // 📏 PIXEL → METERS
    // =========================
    public Point pixelToMeters(Point px) {

        if (px == null || metersPerPixel == 0) return null;

        double mx = px.getX() * metersPerPixel;
        double my = px.getY() * metersPerPixel;

        return new Point(mx, my);
    }

    // =========================
    // 🎯 NORMALIZED → METERS
    // =========================
    public Point normToMeters(Point norm) {

        Point px = normToPixel(norm);
        return pixelToMeters(px);
    }

    // =========================
    // 🌍 NORMALIZED → WORLD (FERRO CENTRO)
    // =========================
    public Point normToWorld(Point norm) {

        if (norm == null || hoopNorm == null) return null;

        Point ballPx = normToPixel(norm);

        Point hoopPx = normToPixel(new Point(
                hoopNorm.center.getX(),
                hoopNorm.center.getY()
        ));

        // 🔥 coordinate relative al ferro
        double dx = ballPx.getX() - hoopPx.getX();
        double dy = hoopPx.getY() - ballPx.getY(); // invertiamo Y

        double mx = dx * metersPerPixel;
        double my = dy * metersPerPixel;

        return new Point(mx, my);
    }

    // =========================
    // 🧪 VALIDATION
    // =========================
    public boolean isNormalized() {

        if (ballNorm != null) {
            for (BallPointDTO p : ballNorm) {
                if (p.getX() > 1 || p.getY() > 1) return false;
            }
        }

        if (releaseNorm != null) {
            if (releaseNorm.getX() > 1 || releaseNorm.getY() > 1) return false;
        }

        if (flightArcNorm != null) {
            for (Point p : flightArcNorm) {
                if (p.getX() > 1 || p.getY() > 1) return false;
            }
        }

        return true;
    }

    // =========================
    // 📋 LOG
    // =========================
    public void logState(Logger log) {

        log.info("📦 ShotContext state:");
        log.infof("Frames: %d", frames != null ? frames.size() : 0);
        log.infof("Ball points: %d", ballNorm != null ? ballNorm.size() : 0);
        log.infof("Flight arc: %d", flightArcNorm != null ? flightArcNorm.size() : 0);

        log.infof("Frame size: %dx%d", frameWidth, frameHeight);
        log.infof("Aspect ratio correction: %.3f", (double) frameWidth / frameHeight);

        log.infof("Hoop diameter (px): %.2f", hoopPixelDiameter);
        log.infof("Meters per pixel: %.5f", metersPerPixel);

        if (!isNormalized()) {
            log.warn("⚠️ NON-normalized data detected!");
        } else {
            log.info("✅ All coordinates are normalized (0–1)");
        }
    }

    public void initFrameSize(Logger log) {
        try {
            BufferedImage first = ImageIO.read(frames.get(0));
            this.frameWidth = first.getWidth();
            this.frameHeight = first.getHeight();

            log.infof("Frame size initialized: %dx%d", frameWidth, frameHeight);

        } catch (IOException e) {
            throw new RuntimeException("Cannot read first frame", e);
        }
    }
}