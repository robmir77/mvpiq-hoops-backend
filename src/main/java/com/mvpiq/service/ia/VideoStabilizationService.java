package com.mvpiq.service.ia;

import ai.djl.modality.cv.output.Point;
import jakarta.enterprise.context.ApplicationScoped;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class VideoStabilizationService {

    public List<AffineTransform> computeTransforms(List<File> frames) {

        List<AffineTransform> transforms = new ArrayList<>();

        if (frames == null || frames.isEmpty()) {
            return transforms;
        }

        double cumulativeDx = 0;
        double cumulativeDy = 0;

        Point prevCenter = null;

        for (int i = 0; i < frames.size(); i++) {
            try {
                BufferedImage img = ImageIO.read(frames.get(i));

                if (img == null) {
                    transforms.add(new AffineTransform());
                    continue;
                }

                double cx = img.getWidth() / 2.0;
                double cy = img.getHeight() / 2.0;

                Point currentCenter = new Point(cx, cy);

                if (prevCenter != null) {
                    double dx = currentCenter.getX() - prevCenter.getX();
                    double dy = currentCenter.getY() - prevCenter.getY();

                    cumulativeDx += dx;
                    cumulativeDy += dy;
                }

                AffineTransform transform = new AffineTransform();
                transform.translate(-cumulativeDx, -cumulativeDy);

                transforms.add(transform);

                prevCenter = currentCenter;

            } catch (Exception e) {
                transforms.add(new AffineTransform());
            }
        }

        return transforms;
    }

    public Point stabilizePoint(Point p, AffineTransform transform) {
        double[] src = new double[]{p.getX(), p.getY()};
        double[] dst = new double[2];

        transform.transform(src, 0, dst, 0, 1);

        return new Point(dst[0], dst[1]);
    }
}