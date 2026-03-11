package com.mvpiq.service;

import jakarta.enterprise.context.ApplicationScoped;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_imgproc.Vec3fVector;

import static org.bytedeco.opencv.global.opencv_cudaarithm.inRange;
import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;

@ApplicationScoped
public class BasketballShotAnalysisService {

    /**
     * Analizza la traiettoria del tiro in un video laterale.
     *
     * @param frames lista dei frame estratti dal video
     * @return lista di centri della palla per frame
     */
    public List<Point> analyzeShot(List<File> frames) {
        if (frames == null || frames.isEmpty()) {
            throw new RuntimeException("No frames to analyze");
        }

        List<Point> ballPositions = new ArrayList<>();
        Point hoopCenter = null;
        int hoopRadius = 0;
        boolean hoopDetected = false;

        for (File frameFile : frames) {
            Mat frame = imread(frameFile.getAbsolutePath());
            if (frame.empty()) continue;

            // Converti in grayscale e sfuma
            Mat gray = new Mat();
            cvtColor(frame, gray, COLOR_BGR2GRAY);
            GaussianBlur(gray, gray, new Size(5, 5), 0);

            // Rilevazione del canestro solo nel primo frame
            if (!hoopDetected) {
                Vec3fVector circles = new Vec3fVector();
                HoughCircles(gray, circles, HOUGH_GRADIENT, 1.0, gray.rows() / 8.0, 100, 30, 20, 50);

                if (circles.size() > 0) {
                    float[] c = new float[3];
                    circles.get(0).get(c);
                    hoopCenter = new Point(Math.round(c[0]), Math.round(c[1]));
                    hoopRadius = Math.round(c[2]);
                    hoopDetected = true;
                }
            }

            // Rilevazione palla (HSV arancione)
            Mat hsv = new Mat();
            cvtColor(frame, hsv, COLOR_BGR2HSV);

            Mat mask = new Mat();
            inRange(hsv, new Scalar(5, 150, 150, 0), new Scalar(15, 255, 255, 0), mask);

            // Trova contorni
            MatVector contours = new MatVector();
            findContours(mask.clone(), contours, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);

            Point frameBallCenter = null;
            double maxArea = 0;

            for (int i = 0; i < contours.size(); i++) {
                Mat cnt = contours.get(i);
                double area = contourArea(cnt);
                if (area < 50 || area > 2000) continue; // filtra rumore

                if (area > maxArea) { // prendi il contorno più grande
                    Rect bounding = boundingRect(cnt);
                    frameBallCenter = new Point(
                            bounding.x() + bounding.width() / 2,
                            bounding.y() + bounding.height() / 2
                    );
                    maxArea = area;
                }
            }

            if (frameBallCenter != null) {
                ballPositions.add(frameBallCenter);

                // Debug: disegna pallone e canestro
                circle(frame, frameBallCenter, 10, new Scalar(0, 255, 0, 0), 2, LINE_8, 0);
                if (hoopCenter != null) {
                    circle(frame, hoopCenter, hoopRadius, new Scalar(255, 0, 0, 0), 2, LINE_8, 0);
                }
            }

            // Salva frame debug opzionale
            imwrite(frameFile.getParent() + "/debug-" + frameFile.getName(), frame);
        }

        return ballPositions;
    }
}