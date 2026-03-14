package com.mvpiq.service;

import ai.djl.Application;
import ai.djl.inference.Predictor;
import ai.djl.modality.Classifications;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.BoundingBox;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Rectangle;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import java.io.File;

@ApplicationScoped
public class HoopDetectorAI {

    private static final Logger LOG = Logger.getLogger(HoopDetectorAI.class);

    private ZooModel<Image, DetectedObjects> model;
    private Predictor<Image, DetectedObjects> predictor;

    @PostConstruct
    public void init() {
        try {
            // Carica un modello SSD MobileNet pre-addestrato su COCO
            Criteria<Image, DetectedObjects> criteria =
                    Criteria.builder()
                            .setTypes(Image.class, DetectedObjects.class)
                            .optApplication(Application.CV.OBJECT_DETECTION)
                            .optEngine("PyTorch")
                            .optArtifactId("yolov5")
                            .build();

            model = ModelZoo.loadModel(criteria);
            predictor = model.newPredictor();

            LOG.info("Object detection model loaded successfully");

        } catch (Exception e) {
            LOG.error("Model loading failed", e);
        }
    }

    public Hoop detectHoop(File frameFile) {
        try {
            Image img = ImageFactory.getInstance().fromFile(frameFile.toPath());
            DetectedObjects detections = predictor.predict(img);

            int width = img.getWidth();
            int height = img.getHeight();

            for (Classifications.Classification c : detections.items()) {
                DetectedObjects.DetectedObject obj = (DetectedObjects.DetectedObject) c;

                String name = obj.getClassName();
                LOG.info("Detected: " + name);

                if (name.equals("sports ball")) {
                    LOG.info("Sports ball detected");
                }

                if (name.toLowerCase().contains("hoop") || name.equals("basketball hoop")) {
                    BoundingBox box = obj.getBoundingBox();
                    Rectangle rect = box.getBounds();

                    int x = (int) ((rect.getX() + rect.getWidth() / 2) * width);
                    int y = (int) ((rect.getY() + rect.getHeight() / 2) * height);
                    int r = (int) (rect.getWidth() * width / 2);

                    LOG.info(String.format("Hoop detected at x=%d, y=%d, r=%d", x, y, r));
                    return new Hoop(x, y, r);
                }
            }

        } catch (Exception e) {
            LOG.error("Detection failed", e);
        }

        LOG.info("No hoop detected in the image");
        return null;
    }
}