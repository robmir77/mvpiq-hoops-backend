package com.mvpiq.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.bytedeco.opencv.opencv_imgproc.Vec3fVector;
import org.jboss.logging.Logger;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;

import java.io.File;

@ApplicationScoped
public class HoopDetectorAI {

    private static final Logger LOG = Logger.getLogger(HoopDetectorAI.class);

    public Hoop detectHoop(File frameFile) {

        LOG.info("Detecting hoop with OpenCV");

        if (frameFile == null || !frameFile.exists()) {
            LOG.error("Frame file does not exist: " + frameFile);
            return null;
        }

        LOG.info("Frame path: " + frameFile.getAbsolutePath());
        LOG.info("Frame size: " + frameFile.length());

        // Carica immagine
        Mat image = opencv_imgcodecs.imread(frameFile.getAbsolutePath());
        if (image.empty()) {
            LOG.error("Failed to load frame image");
            return null;
        }

        // Converti in scala di grigi
        Mat gray = new Mat();
        opencv_imgproc.cvtColor(image, gray, opencv_imgproc.COLOR_BGR2GRAY);

        // Sfoca per ridurre rumore
        opencv_imgproc.GaussianBlur(gray, gray, new Size(9, 9), 2, 2, 0);

        // Cerchi trovati
        Vec3fVector circles = new Vec3fVector();

        try {
            opencv_imgproc.HoughCircles(
                    gray,
                    circles,
                    opencv_imgproc.HOUGH_GRADIENT,
                    1.0,
                    gray.rows() / 8.0, // distanza minima tra centri
                    100.0,  // param1: threshold edge detection
                    30.0,   // param2: threshold accumulator
                    10,     // minRadius
                    200     // maxRadius
            );
        } catch (Exception e) {
            LOG.error("HoughCircles failed", e);
            return null;
        }

        LOG.info("Circles detected: " + circles.size());

        if (circles.size() > 0) {
            float[] circle = new float[3];
            circles.get(0).get(circle);

            int x = Math.round(circle[0]);
            int y = Math.round(circle[1]);
            int r = Math.round(circle[2]);

            LOG.infof("Hoop detected -> x=%d y=%d radius=%d", x, y, r);

            return new Hoop(x, y, r);
        } else {
            LOG.warn("No hoop detected in this frame");
            return null;
        }
    }
}