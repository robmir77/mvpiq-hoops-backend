package com.mvpiq.service.ai;

import ai.djl.Application;
import ai.djl.inference.Predictor;
import ai.djl.modality.Classifications;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.BoundingBox;
import ai.djl.modality.cv.output.Rectangle;
import ai.djl.modality.cv.translator.YoloV5Translator;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;

import ai.djl.training.util.ProgressBar;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import java.io.File;
import java.nio.file.Paths;

@ApplicationScoped
public class HoopDetectionService {

    private static final Logger LOG = Logger.getLogger(HoopDetectionService.class);

    private ZooModel<Image, DetectedObjects> model;
    private Predictor<Image, DetectedObjects> predictor;

    private static final float CONFIDENCE_THRESHOLD = 0.35f;

    @PostConstruct
    public void init() {

        try {

            LOG.info("Loading object detection model...");

            Criteria<Image, DetectedObjects> criteria = Criteria.builder()
                    .setTypes(Image.class, DetectedObjects.class)
                    .optModelPath(Paths.get("C:/mvpiq-hoops/mvpiq-hoops-backend/src/main/resources/model/yolov5s.onnx"))
                    .optEngine("OnnxRuntime")
                    .optTranslator(YoloV5Translator.builder().build())
                    .optProgress(new ProgressBar())
                    .build();

            /* Criteria<Image, DetectedObjects> criteria =
                    Criteria.builder()
                            .setTypes(Image.class, DetectedObjects.class)
                            .optApplication(Application.CV.OBJECT_DETECTION)
                            .optEngine("PyTorch")
                            .optFilter("backbone", "yolov5s")
                            .optProgress(new ai.djl.training.util.ProgressBar())
                            .build(); */

            model = ModelZoo.loadModel(criteria);
            predictor = model.newPredictor();

            LOG.info("Object detection model loaded successfully");

        } catch (Exception e) {

            LOG.error("Model loading failed", e);
        }
    }

    public Hoop detectHoop(File frameFile) {

        if (predictor == null) {

            LOG.warn("Predictor not initialized");
            return null;
        }

        try {

            Image img = ImageFactory.getInstance().fromFile(frameFile.toPath());

            DetectedObjects detections = predictor.predict(img);

            int width = img.getWidth();
            int height = img.getHeight();

            for (Classifications.Classification c : detections.items()) {
                DetectedObjects.DetectedObject obj = (DetectedObjects.DetectedObject) c;

                String name = obj.getClassName();
                double prob = obj.getProbability();

                LOG.infof("Detected: %s (%.2f)", name, prob);

                if (prob < CONFIDENCE_THRESHOLD) {
                    continue;
                }

                // COCO NON HA "HOOP"
                // quindi possiamo solo cercare oggetti compatibili
                if (name.equals("sports ball")) {

                    LOG.info("Basketball detected (possible reference)");

                    BoundingBox box = obj.getBoundingBox();
                    Rectangle rect = box.getBounds();

                    int x = (int) ((rect.getX() + rect.getWidth() / 2) * width);
                    int y = (int) ((rect.getY() + rect.getHeight() / 2) * height);

                    int r = (int) (rect.getWidth() * width / 2);

                    LOG.infof("Ball detected at x=%d y=%d r=%d", x, y, r);

                    // non è il hoop ma può essere usato per calibrazione
                }

                // FUTURO: modello custom "basketball hoop"
                if (name.toLowerCase().contains("hoop") || name.toLowerCase().contains("rim")) {

                    BoundingBox box = obj.getBoundingBox();
                    Rectangle rect = box.getBounds();

                    int x = (int) ((rect.getX() + rect.getWidth() / 2) * width);
                    int y = (int) ((rect.getY() + rect.getHeight() / 2) * height);

                    int r = (int) (rect.getWidth() * width / 2);

                    LOG.infof("Hoop detected at x=%d y=%d r=%d", x, y, r);

                    return new Hoop(x, y, r);
                }
            }

        } catch (Exception e) {

            LOG.error("Hoop detection failed", e);
        }

        LOG.info("No hoop detected in the image");

        return null;
    }
}