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
import jakarta.annotation.PostConstruct;
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

    private Predictor<Image, DetectedObjects> predictor;

    @PostConstruct
    void init() {

        try {

            LOG.info("🧠 Loading YOLOv5 model...");

            Criteria<Image, DetectedObjects> criteria = Criteria.builder()
                    .setTypes(Image.class, DetectedObjects.class)
                    .optModelPath(Paths.get("C:/mvpiq-hoops/mvpiq-hoops-backend/src/main/resources/model/yolov5s.onnx"))
                    .optEngine("OnnxRuntime")
                    .optTranslator(YoloV5Translator.builder().build())
                    .optProgress(new ProgressBar())
                    .build();

            ZooModel<Image, DetectedObjects> model = ModelZoo.loadModel(criteria);

            predictor = model.newPredictor();

            LOG.info("✅ YOLOv5 model loaded successfully");

        } catch (Exception e) {

            LOG.error("❌ Failed to load YOLO model", e);
        }
    }

    public List<Point> trackBallAI(List<File> frames)
            throws IOException, TranslateException {

        List<Point> ballPositions = new ArrayList<>();

        if (frames == null || frames.isEmpty()) {

            LOG.warn("No frames received for ball tracking");
            return ballPositions;
        }

        LOG.info("🏀 Starting AI ball tracking...");
        LOG.info("📸 Frames received: " + frames.size());

        int frameIndex = 0;
        int detectionsCount = 0;

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

            Image image = ImageFactory.getInstance().fromImage(img);

            DetectedObjects detections = predictor.predict(image);

            boolean ballFoundInFrame = false;

            for (int i = 0; i < detections.getNumberOfObjects(); i++) {

                DetectedObjects.DetectedObject obj = detections.item(i);

                if (!obj.getClassName().equalsIgnoreCase("sports ball"))
                    continue;

                double probability = obj.getProbability();

                if (probability < 0.4)
                    continue;

                BoundingBox box = obj.getBoundingBox();
                Rectangle rect = box.getBounds();

                double cx = (rect.getX() + rect.getWidth() / 2) * img.getWidth();
                double cy = (rect.getY() + rect.getHeight() / 2) * img.getHeight();

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

        LOG.info("📊 Ball tracking completed");
        LOG.info("📊 Frames processed: " + frameIndex);
        LOG.info("📊 Ball detections: " + detectionsCount);
        LOG.info("📊 Trajectory points collected: " + ballPositions.size());

        return ballPositions;
    }
}