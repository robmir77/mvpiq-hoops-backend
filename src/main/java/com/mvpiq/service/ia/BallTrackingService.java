package com.mvpiq.service.ia;

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
import com.mvpiq.dto.BallPointDTO;
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

            boolean ballFoundInFrame = false;

            for (int i = 0; i < detections.getNumberOfObjects(); i++) {

                DetectedObjects.DetectedObject obj = detections.item(i);

                if (!obj.getClassName().equalsIgnoreCase("sports ball"))
                    continue;

                double probability = obj.getProbability();

                // threshold più permissivo
                if (probability < 0.25)
                    continue;

                BoundingBox box = obj.getBoundingBox();
                Rectangle rect = box.getBounds();

                double cx = rect.getX() + rect.getWidth() / 2;
                double cy = rect.getY() + rect.getHeight() / 2;

                // Fix coordinate YOLO normalizzate
                if (cx <= 1.5 && cy <= 1.5) {

                    cx *= img.getWidth();
                    cy *= img.getHeight();
                }

                // filtro outlier
                if (lastPoint != null) {

                    double dx = Math.abs(cx - lastPoint.getX());
                    double dy = Math.abs(cy - lastPoint.getY());

                    if (dx > 300 || dy > 300) {

                        LOG.infof("❌ Outlier ball detection ignored frame=%d", frameIndex);
                        continue;
                    }
                }

                BallPointDTO p = new BallPointDTO(cx, cy, i);

                ballPositions.add(p);
                lastPoint = p;

                detectionsCount++;
                ballFoundInFrame = true;

                LOG.infof(
                        "🏀 Ball detected -> frame=%d x=%.2f y=%.2f confidence=%.3f",
                        frameIndex,
                        p.getX(),
                        p.getY(),
                        probability
                );
            }

            // se YOLO perde la palla stimiamo posizione
            if (!ballFoundInFrame && lastPoint != null) {

                ballPositions.add(lastPoint);

                LOG.infof(
                        "📈 Interpolated ball position frame=%d x=%.2f y=%.2f",
                        frameIndex,
                        lastPoint.getX(),
                        lastPoint.getY()
                );
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