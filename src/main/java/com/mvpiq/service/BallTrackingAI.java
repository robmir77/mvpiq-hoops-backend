package com.mvpiq.service;

import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.BoundingBox;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Rectangle;
import ai.djl.modality.cv.translator.YoloV5Translator;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import javax.imageio.ImageIO;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class BallTrackingAI {

    private static final Logger LOG = Logger.getLogger(BallTrackingAI.class);

    public List<Point> trackBallAI(List<File> frames)
            throws IOException, ModelException, TranslateException {

        if (frames == null || frames.isEmpty()) {
            LOG.warn("No frames received for ball tracking");
            return new ArrayList<>();
        }

        LOG.info("🏀 Starting AI ball tracking...");
        LOG.info("📸 Frames received: " + frames.size());

        Criteria<Image, DetectedObjects> criteria = Criteria.builder()
                .setTypes(Image.class, DetectedObjects.class)
                .optModelPath(Paths.get("C:/mvpiq-hoops/mvpiq-hoops-backend/src/main/resources/model/yolov5s.onnx"))
                .optEngine("OnnxRuntime")
                .optTranslator(YoloV5Translator.builder().build())
                .optProgress(new ProgressBar())
                .build();

        List<Point> ballPositions = new ArrayList<>();
        int frameIndex = 0;
        int detectionsCount = 0;

        LOG.info("🧠 Loading YOLOv5 model...");

        try (ZooModel<Image, DetectedObjects> model = ModelZoo.loadModel(criteria);
             Predictor<Image, DetectedObjects> predictor = model.newPredictor()) {

            LOG.info("✅ Model loaded successfully");

            for (File frameFile : frames) {

                frameIndex++;

                LOG.infof("🔍 Processing frame %d / %d -> %s",
                        frameIndex,
                        frames.size(),
                        frameFile.getName());

                BufferedImage img;

                try {
                    img = ImageIO.read(frameFile);
                } catch (Exception e) {
                    LOG.errorf(e, "❌ Failed reading frame %s", frameFile.getName());
                    continue;
                }

                if (img == null) {
                    LOG.warn("⚠️ Frame image is null: " + frameFile.getName());
                    continue;
                }

                LOG.debugf("Frame size: %dx%d", img.getWidth(), img.getHeight());

                Image image = ImageFactory.getInstance().fromImage(img);

                DetectedObjects detections = predictor.predict(image);

                LOG.debugf("Objects detected in frame %d: %d",
                        frameIndex,
                        detections.getNumberOfObjects());

                boolean ballFoundInFrame = false;

                for (int i = 0; i < detections.getNumberOfObjects(); i++) {

                    DetectedObjects.DetectedObject obj = detections.item(i);

                    String className = obj.getClassName();
                    double probability = obj.getProbability();

                    LOG.debugf(
                            "Detected object -> class=%s prob=%.3f",
                            className,
                            probability
                    );

                    if (!className.equalsIgnoreCase("sports ball")) {
                        continue;
                    }

                    if (probability < 0.4) {
                        LOG.debug("Ignoring low confidence ball detection");
                        continue;
                    }

                    BoundingBox box = obj.getBoundingBox();
                    Rectangle rect = box.getBounds();

                    LOG.debugf(
                            "Bounding box raw -> x=%.5f y=%.5f w=%.5f h=%.5f",
                            rect.getX(),
                            rect.getY(),
                            rect.getWidth(),
                            rect.getHeight()
                    );

                    double cx;
                    double cy;

                    if (rect.getWidth() <= 1 && rect.getHeight() <= 1) {

                        // coordinate normalizzate
                        cx = (rect.getX() + rect.getWidth() / 2) * img.getWidth();
                        cy = (rect.getY() + rect.getHeight() / 2) * img.getHeight();

                        LOG.debug("Using normalized coordinates conversion");

                    } else {

                        // coordinate in pixel
                        cx = rect.getX() + rect.getWidth() / 2;
                        cy = rect.getY() + rect.getHeight() / 2;

                        LOG.debug("Using pixel coordinates conversion");
                    }

                    Point p = new Point((int) cx, (int) cy);
                    ballPositions.add(p);

                    detectionsCount++;
                    ballFoundInFrame = true;

                    LOG.infof(
                            "🏀 Ball detected -> frame=%d x=%d y=%d confidence=%.3f",
                            frameIndex,
                            p.x,
                            p.y,
                            probability
                    );
                }

                if (!ballFoundInFrame) {
                    LOG.debugf("No ball detected in frame %d", frameIndex);
                }
            }
        }

        LOG.info("📊 Ball tracking completed");
        LOG.info("📊 Frames processed: " + frameIndex);
        LOG.info("📊 Ball detections: " + detectionsCount);
        LOG.info("📊 Trajectory points collected: " + ballPositions.size());

        if (ballPositions.isEmpty()) {
            LOG.warn("⚠️ No ball trajectory detected!");
        } else {

            for (int i = 0; i < ballPositions.size(); i++) {

                Point p = ballPositions.get(i);

                LOG.debugf(
                        "Trajectory point %d -> x=%d y=%d",
                        i,
                        p.x,
                        p.y
                );
            }
        }

        return ballPositions;
    }
}