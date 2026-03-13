package com.mvpiq.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.bytedeco.javacpp.BytePointer;
import org.jboss.logging.Logger;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_imgproc.Vec3fVector;

import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.global.opencv_imgcodecs;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import com.twelvemonkeys.imageio.plugins.jpeg.JPEGImageReaderSpi;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.io.File;

@ApplicationScoped
public class HoopDetectorAI {

    private static final Logger LOG = Logger.getLogger(HoopDetectorAI.class);

    public Hoop detectHoop(File frameFile) {
        LOG.info("========== HOOP DETECTION START ==========");

        try {
            if (frameFile == null || !frameFile.exists() || frameFile.length() == 0) {
                LOG.error("Frame file is missing or empty");
                return null;
            }

            LOG.infof("Frame path: %s, size=%d bytes", frameFile.getAbsolutePath(), frameFile.length());

            // -------------------------
            // REGISTER TWELVEMONKEYS JPEG PLUGIN
            // -------------------------
            IIORegistry registry = IIORegistry.getDefaultInstance();
            registry.registerServiceProvider(new JPEGImageReaderSpi());

            // -------------------------
            // LOAD IMAGE VIA IMAGEIO
            // -------------------------
            BufferedImage bufferedImage = ImageIO.read(frameFile);
            if (bufferedImage == null) {
                LOG.error("ImageIO could not read image (TwelveMonkeys applied)");
                return null;
            }
            LOG.infof("ImageIO read success -> width=%d height=%d", bufferedImage.getWidth(), bufferedImage.getHeight());

            // -------------------------
            // CONVERT BufferedImage TO Mat
            // -------------------------
            Mat image = bufferedImageToMat(bufferedImage);
            LOG.infof("Converted BufferedImage to Mat -> cols=%d rows=%d", image.cols(), image.rows());
            opencv_imgcodecs.imwrite("C:\\Temp\\debug_original.png", image);

            // -------------------------
            // GRAYSCALE
            // -------------------------
            Mat gray = new Mat();
            opencv_imgproc.cvtColor(image, gray, opencv_imgproc.COLOR_BGR2GRAY);
            LOG.infof("Grayscale done -> cols=%d rows=%d", gray.cols(), gray.rows());
            opencv_imgcodecs.imwrite("C:\\Temp\\debug_gray.png", gray);

            // -------------------------
            // BLUR
            // -------------------------
            opencv_imgproc.GaussianBlur(gray, gray, new Size(7, 7), 1.5);
            LOG.info("Gaussian blur applied");
            opencv_imgcodecs.imwrite("C:\\Temp\\debug_blur.png", gray);

            // -------------------------
            // EDGE DETECTION
            // -------------------------
            Mat edges = new Mat();
            opencv_imgproc.Canny(gray, edges, 50, 150);
            LOG.info("Canny edge detection done");
            opencv_imgcodecs.imwrite("C:\\Temp\\debug_edges.png", edges);

            // -------------------------
            // ROI
            // -------------------------
            int roiTop = image.rows() / 8;
            int roiHeight = image.rows() / 3;
            Rect roiRect = new Rect(0, roiTop, image.cols(), roiHeight);
            Mat roi = new Mat(edges, roiRect);
            LOG.infof("ROI extracted -> top=%d height=%d cols=%d rows=%d", roiTop, roiHeight, roi.cols(), roi.rows());
            opencv_imgcodecs.imwrite("C:\\Temp\\debug_roi.png", roi);

            // -------------------------
            // HOUGH CIRCLES
            // -------------------------
            Vec3fVector circles = new Vec3fVector();
            opencv_imgproc.HoughCircles(
                    roi,
                    circles,
                    opencv_imgproc.HOUGH_GRADIENT,
                    1.5,
                    80,
                    100,
                    20,
                    12,
                    80
            );
            LOG.infof("Circles detected: %d", circles.size());

            if (circles.size() == 0) {
                LOG.warn("No circles detected");
                return null;
            }

            // -------------------------
            // FIND BEST CIRCLE
            // -------------------------
            int bestIndex = -1;
            float bestRadius = 0;
            for (int i = 0; i < circles.size(); i++) {
                float[] c = new float[3];
                circles.get(i).get(c);
                LOG.infof("Circle %d -> x=%.1f y=%.1f r=%.1f", i, c[0], c[1], c[2]);
                if (c[2] > bestRadius) {
                    bestRadius = c[2];
                    bestIndex = i;
                }
            }

            float[] best = new float[3];
            circles.get(bestIndex).get(best);
            int x = Math.round(best[0]);
            int y = Math.round(best[1] + roiTop);
            int r = Math.round(best[2]);

            LOG.infof("HOOP DETECTED -> x=%d y=%d r=%d", x, y, r);
            LOG.info("========== HOOP DETECTION END ==========");

            return new Hoop(x, y, r);

        } catch (Exception e) {
            LOG.error("Hoop detection crashed", e);
            return null;
        }
    }

    private Mat bufferedImageToMat(BufferedImage bi) {

        LOG.info("---- BufferedImage → Mat conversion START ----");

        if (bi == null) {
            LOG.error("BufferedImage is NULL");
            return new Mat();
        }

        try {

            LOG.infof(
                    "BufferedImage info -> width=%d height=%d type=%d",
                    bi.getWidth(),
                    bi.getHeight(),
                    bi.getType()
            );

            // ------------------------------------------------
            // Convertiamo sempre in TYPE_3BYTE_BGR
            // (formato più compatibile con OpenCV)
            // ------------------------------------------------
            BufferedImage converted = new BufferedImage(
                    bi.getWidth(),
                    bi.getHeight(),
                    BufferedImage.TYPE_3BYTE_BGR
            );

            converted.getGraphics().drawImage(bi, 0, 0, null);

            DataBuffer buffer = converted.getRaster().getDataBuffer();
            LOG.info("DataBuffer class: " + buffer.getClass().getName());

            byte[] pixels = ((DataBufferByte) buffer).getData();

            LOG.info("Pixel array length = " + pixels.length);

            // ------------------------------------------------
            // Creiamo la Mat usando direttamente il buffer
            // ------------------------------------------------
            Mat mat = new Mat(
                    converted.getHeight(),
                    converted.getWidth(),
                    opencv_core.CV_8UC3,
                    new BytePointer(pixels)
            );

            LOG.infof(
                    "Mat created -> rows=%d cols=%d channels=%d",
                    mat.rows(),
                    mat.cols(),
                    mat.channels()
            );

            LOG.info("Cloning Mat to detach from Java buffer");

            Mat result = mat.clone();

            LOG.info("---- BufferedImage → Mat conversion END ----");

            return result;

        } catch (Exception e) {

            LOG.error("BufferedImage → Mat conversion FAILED", e);

            return new Mat();
        }
    }
}