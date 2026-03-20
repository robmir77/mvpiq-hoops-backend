package com.mvpiq.service.ia;

import ai.djl.inference.Predictor;
import ai.djl.modality.Classifications;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.BoundingBox;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Point;
import ai.djl.modality.cv.output.Rectangle;
import ai.djl.modality.cv.translator.YoloV5Translator;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;
import com.mvpiq.dto.BallPointDTO;
import com.mvpiq.dto.KalmanBallFilterDTO;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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

    @Inject
    VideoStabilizationService stabilizationService;

    private Predictor<Image, DetectedObjects> predictor;

    @PostConstruct
    void init() {

        try {

            LOG.info("🧠 Loading YOLO model...");

            Criteria<Image, DetectedObjects> criteria = Criteria.builder()
                    .setTypes(Image.class, DetectedObjects.class)
                    .optModelPath(Paths.get("models/yolov5s.onnx"))
                    .optEngine("OnnxRuntime")
                    .optTranslator(YoloV5Translator.builder().build())
                    .optProgress(new ProgressBar())
                    .build();

            ZooModel<Image, DetectedObjects> model = ModelZoo.loadModel(criteria);

            predictor = model.newPredictor();

            LOG.info("✅ YOLO model loaded successfully");

        } catch (Exception e) {

            LOG.error("❌ Failed to load YOLO model", e);
        }
    }

    public List<BallPointDTO> trackBallAI(ShotContext context) throws TranslateException {

        List<File> frames = context.frames;

        KalmanBallFilterDTO kalman = new KalmanBallFilterDTO();
        List<BallPointDTO> ballPositions = new ArrayList<>();

        if (frames == null || frames.isEmpty()) {
            LOG.warn("No frames received for ball tracking");
            return ballPositions;
        }

        LOG.info("🏀 Starting AI ball tracking...");
        LOG.info("📸 Frames received: " + frames.size());

        int frameIndex = 0;
        int detectionsCount = 0;
        int missingFrames = 0;

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

            int width = img.getWidth();
            int height = img.getHeight();

            Image image = ImageFactory.getInstance().fromImage(img);
            DetectedObjects detections = predictor.predict(image);

            // -------------------------
            // DEBUG DETECTIONS
            // -------------------------
            for (Classifications.Classification obj : detections.items()) {
                LOG.infof("Detected -> class=%s prob=%.3f",
                        obj.getClassName(),
                        obj.getProbability());
            }

            boolean ballFoundInFrame = false;
            DetectedObjects.DetectedObject bestBall = null;
            double bestScore = -1.0;

            // -------------------------
            // POSIZIONE ATTESA (Kalman prediction) - NORMALIZED
            // -------------------------
            Double expectedX = null;
            Double expectedY = null;

            if (kalman.isInitialized()) {
                expectedX = kalman.getX();
                expectedY = kalman.getY();
            }

            for (DetectedObjects.DetectedObject obj : detections.<DetectedObjects.DetectedObject>items()) {

                String cls = obj.getClassName().toLowerCase();

                // -------------------------
                // FILTRO CLASSE
                // -------------------------
                boolean isBall = cls.contains("ball");
                boolean fallback = (cls.contains("sports") || cls.contains("round")) && obj.getProbability() > 0.5;

                if (!(isBall || fallback)) continue;

                double probability = obj.getProbability();
                if (probability < 0.1) continue;

                BoundingBox box = obj.getBoundingBox();
                Rectangle rect = box.getBounds();

                double cx = rect.getX() + rect.getWidth() / 2.0;
                double cy = rect.getY() + rect.getHeight() / 2.0;
                double bw = rect.getWidth();
                double bh = rect.getHeight();

                // -------------------------
                // NORMALIZZAZIONE UNIFICATA (SEMPRE 0–1)
                // -------------------------
                boolean alreadyNormalized = bw <= 1.0 && bh <= 1.0;

                if (!alreadyNormalized) {
                    cx /= width;
                    cy /= height;
                    bw /= width;
                    bh /= height;
                }

                // -------------------------
                // 1) FILTRO DIMENSIONE (NORMALIZED)
                // -------------------------
                if (bw < 0.005 || bh < 0.005 || bw > 0.2 || bh > 0.2) {
                    LOG.infof("❌ Candidate ignored (size) frame=%d w=%.4f h=%.4f",
                            frameIndex, bw, bh);
                    continue;
                }

                // -------------------------
                // 2) FILTRO FORMA
                // -------------------------
                double aspectRatio = bw / bh;
                if (aspectRatio < 0.5 || aspectRatio > 3) {
                    LOG.infof("❌ Candidate ignored (aspect ratio) frame=%d ar=%.2f",
                            frameIndex, aspectRatio);
                    continue;
                }

                // -------------------------
                // 3) DISTANZA DINAMICA (NORMALIZED)
                // -------------------------
                double distanceScore = 1.0;
                double distance = 0.0;

                if (expectedX != null && expectedY != null) {
                    double dx = cx - expectedX;
                    double dy = cy - expectedY;
                    distance = Math.sqrt(dx * dx + dy * dy);

                    double maxDistance = 0.15 + (missingFrames * 0.05);

                    if (distance > maxDistance) {
                        LOG.infof("❌ Candidate ignored (too far) frame=%d dist=%.4f max=%.4f",
                                frameIndex, distance, maxDistance);
                        continue;
                    }

                    distanceScore = Math.max(0.0, 1.0 - (distance / maxDistance));
                }

                // -------------------------
                // 4) SCORE
                // -------------------------
                double shapeScore = 1.0 - Math.abs(1.0 - aspectRatio);

                double score = (probability * 0.4)
                        + (distanceScore * 0.4)
                        + (shapeScore * 0.2);

                LOG.infof("🟡 Candidate frame=%d x=%.4f y=%.4f conf=%.3f dist=%.4f score=%.3f",
                        frameIndex, cx, cy, probability, distance, score);

                if (score > bestScore) {
                    bestScore = score;
                    bestBall = obj;
                }
            }

            // -------------------------
            // ACCETTAZIONE
            // -------------------------
            if (bestBall != null && bestScore >= 0.35) {

                BoundingBox box = bestBall.getBoundingBox();
                Rectangle rect = box.getBounds();

                double cx = rect.getX() + rect.getWidth() / 2.0;
                double cy = rect.getY() + rect.getHeight() / 2.0;

                boolean alreadyNormalized = rect.getWidth() <= 1.0 && rect.getHeight() <= 1.0;

                if (!alreadyNormalized) {
                    cx /= width;
                    cy /= height;
                }

                // -------------------------
                // STABILIZZAZIONE (lavora in pixel → convertiamo)
                // -------------------------
                Point stabilized;

                if (context.stabilized) {

                    Point pixelPoint = new Point(cx * width, cy * height);

                    pixelPoint = stabilizationService.stabilizePoint(
                            pixelPoint,
                            context.frameTransforms.get(frameIndex - 1)
                    );

                    cx = pixelPoint.getX() / width;
                    cy = pixelPoint.getY() / height;
                }

                // -------------------------
                // KALMAN (UNA SOLA VOLTA, NORMALIZED)
                // -------------------------
                if (!kalman.isInitialized()) {
                    kalman.init(cx, cy);
                    LOG.info("🧠 Kalman filter initialized");
                } else {
                    kalman.update(cx, cy);
                }

                BallPointDTO p = new BallPointDTO(
                        kalman.getX(),
                        kalman.getY(),
                        frameIndex
                );

                ballPositions.add(p);

                detectionsCount++;
                ballFoundInFrame = true;
                missingFrames = 0;

                LOG.infof("🏀 Ball accepted -> frame=%d x=%.4f y=%.4f score=%.3f",
                        frameIndex, p.getX(), p.getY(), bestScore);
            }

            // -------------------------
            // PREDIZIONE LIMITATA
            // -------------------------
            if (!ballFoundInFrame && kalman.isInitialized()) {

                if (missingFrames < 5) {
                    kalman.predict();

                    BallPointDTO predicted = new BallPointDTO(
                            kalman.getX(),
                            kalman.getY(),
                            frameIndex
                    );

                    ballPositions.add(predicted);

                    missingFrames++;

                    LOG.infof("🧠 Kalman predicted frame=%d x=%.4f y=%.4f (missing=%d)",
                            frameIndex, predicted.getX(), predicted.getY(), missingFrames);
                } else {
                    LOG.warn("⚠️ Too many missing frames → resetting Kalman");
                    kalman = new KalmanBallFilterDTO();
                    missingFrames = 0;
                }
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