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
        List<BallPointDTO> ballPositions = new ArrayList<>();
        KalmanBallFilterDTO kalman = new KalmanBallFilterDTO();

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
                    frameIndex, frames.size(), frameFile.getName());

            BufferedImage img = readFrame(frameFile);
            if (img == null) continue;

            DetectedObjects detections = detect(img);

            Candidate best = findBestCandidate(
                    detections, img, kalman, missingFrames, frameIndex
            );

            if (best != null) {

                BallPointDTO point = acceptCandidate(
                        best, kalman, context, frameIndex, img
                );

                ballPositions.add(point);

                detectionsCount++;
                missingFrames = 0;

                LOG.infof("🏀 Ball accepted -> frame=%d x=%.4f y=%.4f score=%.3f",
                        frameIndex, point.getX(), point.getY(), best.score);

            } else {

                BallPointDTO predicted = predict(kalman, frameIndex, missingFrames);

                if (predicted != null) {
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
        }

        LOG.info("📊 Ball tracking completed");
        LOG.info("📊 Frames processed: " + frameIndex);
        LOG.info("📊 Ball detections: " + detectionsCount);
        LOG.info("📊 Trajectory points collected: " + ballPositions.size());

        return ballPositions;
    }

    private BufferedImage readFrame(File frameFile) {
        try {
            BufferedImage img = ImageIO.read(frameFile);

            if (img == null) {
                LOG.warn("⚠️ Frame image is null: " + frameFile.getName());
            }

            return img;

        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed reading frame %s", frameFile.getName());
            return null;
        }
    }

    private DetectedObjects detect(BufferedImage img) throws TranslateException {
        Image image = ImageFactory.getInstance().fromImage(img);
        DetectedObjects detections = predictor.predict(image);

        // DEBUG
        for (Classifications.Classification obj : detections.items()) {
            LOG.infof("Detected -> class=%s prob=%.3f",
                    obj.getClassName(),
                    obj.getProbability());
        }

        return detections;
    }

    private Candidate findBestCandidate(DetectedObjects detections,
                                        BufferedImage img,
                                        KalmanBallFilterDTO kalman,
                                        int missingFrames,
                                        int frameIndex) {

        Double expectedX = kalman.isInitialized() ? kalman.getX() : null;
        Double expectedY = kalman.isInitialized() ? kalman.getY() : null;

        Candidate best = null;

        for (DetectedObjects.DetectedObject obj : detections.<DetectedObjects.DetectedObject>items()) {

            if (!isBallCandidate(obj)) continue;

            Candidate c = scoreCandidate(
                    obj,
                    img.getWidth(),
                    img.getHeight(),
                    expectedX,
                    expectedY,
                    missingFrames,
                    frameIndex
            );

            if (best == null || c.score > best.score) {
                best = c;
            }
        }

        if (best != null) {
            LOG.infof("🏆 Best candidate frame=%d score=%.3f", frameIndex, best.score);
        }

        return (best != null && best.score > 0.25) ? best : null;
    }

    private boolean isBallCandidate(DetectedObjects.DetectedObject obj) {
        String cls = obj.getClassName().toLowerCase();

        if (cls.contains("ball")) return true;

        return (cls.contains("sports") || cls.contains("round"))
                && obj.getProbability() > 0.3;
    }

    private Candidate scoreCandidate(DetectedObjects.DetectedObject obj,
                                     int width,
                                     int height,
                                     Double expectedX,
                                     Double expectedY,
                                     int missingFrames,
                                     int frameIndex) {

        Rectangle rect = obj.getBoundingBox().getBounds();

        double cx = rect.getX() + rect.getWidth() / 2.0;
        double cy = rect.getY() + rect.getHeight() / 2.0;
        double bw = rect.getWidth();
        double bh = rect.getHeight();

        // NORMALIZE
        if (bw > 1.0 || bh > 1.0) {
            cx /= width;
            cy /= height;
            bw /= width;
            bh /= height;
        }

        double probability = obj.getProbability();

        // SIZE (soft)
        double sizeScore = (bw < 0.005 || bh < 0.005 || bw > 0.25 || bh > 0.25) ? 0.2 : 1.0;

        // SHAPE
        double aspectRatio = bw / bh;
        double shapeScore = Math.max(0.0, 1.0 - Math.abs(1.0 - aspectRatio));

        // DISTANCE
        double distanceScore = 1.0;
        double distance = 0.0;

        if (expectedX != null) {
            double dx = cx - expectedX;
            double dy = cy - expectedY;
            distance = Math.sqrt(dx * dx + dy * dy);

            double maxDistance = 0.25 + (missingFrames * 0.08);

            distanceScore = Math.max(0.0, 1.0 - (distance / maxDistance));
        }

        double score =
                (probability * 0.5)
                        + (distanceScore * 0.3)
                        + (shapeScore * 0.1)
                        + (sizeScore * 0.1);

        LOG.infof("🟡 Candidate frame=%d x=%.4f y=%.4f prob=%.3f dist=%.4f score=%.3f",
                frameIndex, cx, cy, probability, distance, score);

        Candidate c = new Candidate();
        c.cx = cx;
        c.cy = cy;
        c.score = score;

        return c;
    }

    private BallPointDTO acceptCandidate(Candidate c,
                                         KalmanBallFilterDTO kalman,
                                         ShotContext context,
                                         int frameIndex,
                                         BufferedImage img) {

        double cx = c.cx;
        double cy = c.cy;

        int width = img.getWidth();
        int height = img.getHeight();

        // STABILIZZAZIONE
        if (context.stabilized) {
            Point pixelPoint = new Point(cx * width, cy * height);

            pixelPoint = stabilizationService.stabilizePoint(
                    pixelPoint,
                    context.frameTransforms.get(frameIndex - 1)
            );

            cx = pixelPoint.getX() / width;
            cy = pixelPoint.getY() / height;
        }

        // KALMAN
        if (!kalman.isInitialized()) {
            kalman.init(cx, cy);
            LOG.info("🧠 Kalman filter initialized");
        } else {
            kalman.update(cx, cy);
        }

        return new BallPointDTO(
                kalman.getX(),
                kalman.getY(),
                frameIndex
        );
    }

    private BallPointDTO predict(KalmanBallFilterDTO kalman,
                                 int frameIndex,
                                 int missingFrames) {

        if (!kalman.isInitialized()) return null;

        if (missingFrames >= 5) return null;

        kalman.predict();

        return new BallPointDTO(
                kalman.getX(),
                kalman.getY(),
                frameIndex
        );
    }
}