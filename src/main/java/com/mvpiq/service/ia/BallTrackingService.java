package com.mvpiq.service.ia;

import ai.djl.inference.Predictor;
import ai.djl.modality.Classifications;
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
import com.mvpiq.dto.BallPointDTO;
import com.mvpiq.dto.KalmanBallFilter;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class BallTrackingService {

    private static final Logger LOG = Logger.getLogger(BallTrackingService.class);

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

    public List<BallPointDTO> trackBallAI(List<File> frames) throws TranslateException {

        KalmanBallFilter kalman = new KalmanBallFilter();
        List<BallPointDTO> ballPositions = new ArrayList<>();

        if (frames == null || frames.isEmpty()) {
            LOG.warn("No frames received for ball tracking");
            return ballPositions;
        }

        LOG.info("🏀 Starting AI ball tracking...");
        LOG.info("📸 Frames received: " + frames.size());

        int frameIndex = 0;
        int detectionsCount = 0;
        BallPointDTO lastPoint = null;

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

            // Log utile per debug
            for (Classifications.Classification obj : detections.items()) {
                LOG.infof("Detected -> class=%s prob=%.3f",
                        obj.getClassName(),
                        obj.getProbability());
            }

            boolean ballFoundInFrame = false;
            DetectedObjects.DetectedObject bestBall = null;
            double bestScore = -1.0;

            Double expectedX = null;
            Double expectedY = null;

            if (lastPoint != null) {
                expectedX = lastPoint.getX();
                expectedY = lastPoint.getY();
            }

            for (DetectedObjects.DetectedObject obj : detections.<DetectedObjects.DetectedObject>items()) {
                LOG.infof("Detected object: %s confidence=%.2f x=%.2f y=%.2f",
                        obj.getClassName(),
                        obj.getProbability(),
                        obj.getBoundingBox().getPoint().getX(),
                        obj.getBoundingBox().getPoint().getY());

                String cls = obj.getClassName().toLowerCase();

                if (!(cls.contains("ball") ||
                        cls.contains("racket") ||
                        cls.contains("frisbee"))) {
                    continue;
                }

                double probability = obj.getProbability();
                if (probability < 0.25) {
                    continue;
                }

                BoundingBox box = obj.getBoundingBox();
                Rectangle rect = box.getBounds();

                double cx = rect.getX() + rect.getWidth() / 2.0;
                double cy = rect.getY() + rect.getHeight() / 2.0;
                double bw = rect.getWidth();
                double bh = rect.getHeight();

                // coordinate normalizzate -> pixel
                if (cx <= 1.5 && cy <= 1.5) {
                    cx *= img.getWidth();
                    cy *= img.getHeight();
                }

                if (bw <= 1.5 && bh <= 1.5) {
                    bw *= img.getWidth();
                    bh *= img.getHeight();
                }

                // 1) filtro dimensione
                if (bw < 4 || bh < 4 || bw > 100 || bh > 100) {
                    LOG.infof("❌ Candidate ignored (size) frame=%d w=%.2f h=%.2f conf=%.3f",
                            frameIndex, bw, bh, probability);
                    continue;
                }

                // 2) filtro forma: una palla deve essere circa quadrata
                double aspectRatio = bw / bh;
                if (aspectRatio < 0.6 || aspectRatio > 1.6) {
                    LOG.infof("❌ Candidate ignored (aspect ratio) frame=%d ar=%.2f conf=%.3f",
                            frameIndex, aspectRatio, probability);
                    continue;
                }

                // 3) distanza dalla posizione attesa
                double distanceScore = 1.0;
                double distance = 0.0;

                if (expectedX != null && expectedY != null) {
                    double dx = cx - expectedX;
                    double dy = cy - expectedY;
                    distance = Math.sqrt(dx * dx + dy * dy);

                    // scarto outlier troppo lontani
                    if (distance > 250) {
                        LOG.infof("❌ Candidate ignored (too far) frame=%d dist=%.2f conf=%.3f",
                                frameIndex, distance, probability);
                        continue;
                    }

                    // più vicino = punteggio più alto
                    distanceScore = Math.max(0.0, 1.0 - (distance / 180.0));
                }

                // 4) punteggio finale
                double shapeScore = 1.0 - Math.abs(1.0 - aspectRatio); // massimo quando ar≈1
                double score = (probability * 0.6) + (distanceScore * 0.3) + (shapeScore * 0.1);

                LOG.infof("🟡 Ball candidate frame=%d x=%.2f y=%.2f w=%.2f h=%.2f conf=%.3f dist=%.2f score=%.3f",
                        frameIndex, cx, cy, bw, bh, probability, distance, score);

                if (score > bestScore) {
                    bestScore = score;
                    bestBall = obj;
                }
            }

            // accetto solo se il punteggio finale è abbastanza buono
            if (bestBall != null && bestScore >= 0.35) {
                double probability = bestBall.getProbability();
                BoundingBox box = bestBall.getBoundingBox();
                Rectangle rect = box.getBounds();

                double cx = rect.getX() + rect.getWidth() / 2.0;
                double cy = rect.getY() + rect.getHeight() / 2.0;

                if (cx <= 1.5 && cy <= 1.5) {
                    cx *= img.getWidth();
                    cy *= img.getHeight();
                }

                if (!kalman.isInitialized()) {
                    kalman.init(cx, cy);
                    LOG.info("🧠 Kalman filter initialized");
                } else {
                    kalman.update(cx, cy);
                }

                BallPointDTO p = new BallPointDTO(kalman.getX(), kalman.getY(), frameIndex);
                ballPositions.add(p);
                lastPoint = p;
                detectionsCount++;
                ballFoundInFrame = true;

                LOG.infof("🏀 Ball accepted -> frame=%d x=%.2f y=%.2f confidence=%.3f score=%.3f",
                        frameIndex, p.getX(), p.getY(), probability, bestScore);
            }

            if (!ballFoundInFrame && kalman.isInitialized()) {
                kalman.predict();

                BallPointDTO predicted = new BallPointDTO(kalman.getX(), kalman.getY(), frameIndex);
                ballPositions.add(predicted);
                lastPoint = predicted;

                LOG.infof("🧠 Kalman predicted ball frame=%d x=%.2f y=%.2f",
                        frameIndex, predicted.getX(), predicted.getY());
            }

            if (!ballFoundInFrame) {
                LOG.infof("No ball detected in frame %d", frameIndex);
            }
        }

        LOG.info("📊 Ball tracking completed");
        LOG.info("📊 Frames processed: " + frameIndex);
        LOG.info("📊 Ball detections: " + detectionsCount);
        LOG.info("📊 Trajectory points collected: " + ballPositions.size());

        return ballPositions;
    }
}