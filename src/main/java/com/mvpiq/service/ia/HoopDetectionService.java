package com.mvpiq.service.ia;

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
import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Paths;

import org.bytedeco.opencv.opencv_core.*;

import javax.imageio.ImageIO;

@ApplicationScoped
public class HoopDetectionService {

    private static final Logger LOG = Logger.getLogger(HoopDetectionService.class);

    private ZooModel<Image, DetectedObjects> model;
    private Predictor<Image, DetectedObjects> predictor;

    private static final float CONFIDENCE_THRESHOLD = 0.35f;

    //@PostConstruct
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

    public Hoop detectHoopIA(File frameFile) {

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

    public Hoop detectHoopNear(File frameFile, double centerXdouble, double centerYdouble, int searchRadius) {
        try {
            BufferedImage img = ImageIO.read(frameFile);

            if (img == null) {
                LOG.warn("Image read failed");
                return null;
            }

            int width = img.getWidth();
            int height = img.getHeight();

            int centerX = (int) Math.round(centerXdouble);
            int centerY = (int) Math.round(centerXdouble);

            // Limiti ROI dentro l'immagine
            int minX = Math.max(0, centerX - searchRadius);
            int maxX = Math.min(width - 1, centerX + searchRadius);
            int minY = Math.max(0, centerY - searchRadius);
            int maxY = Math.min(height - 1, centerY + searchRadius);

            int detectedMinX = width;
            int detectedMinY = height;
            int detectedMaxX = 0;
            int detectedMaxY = 0;

            int orangePixels = 0;

            // Scansione solo nel ROI
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {

                    int rgb = img.getRGB(x, y);
                    int r = (rgb >> 16) & 0xff;
                    int g = (rgb >> 8) & 0xff;
                    int b = rgb & 0xff;

                    // Filtro arancione robusto
                    if (r > 120 && g > 60 && b < 120 && r > g && g > b) {

                        orangePixels++;

                        if (x < detectedMinX) detectedMinX = x;
                        if (y < detectedMinY) detectedMinY = y;
                        if (x > detectedMaxX) detectedMaxX = x;
                        if (y > detectedMaxY) detectedMaxY = y;
                    }
                }
            }

            LOG.info("Orange pixels detected in ROI: " + orangePixels);

            if (orangePixels < 10) { // soglia più bassa perché ROI più piccolo
                LOG.warn("Not enough orange pixels in ROI");
                return null;
            }

            int hoopCenterX = (detectedMinX + detectedMaxX) / 2;
            int hoopCenterY = (detectedMinY + detectedMaxY) / 2;
            int radius = Math.max(detectedMaxX - detectedMinX, detectedMaxY - detectedMinY) / 2;

            LOG.infof("Hoop detected in ROI -> x=%d y=%d r=%d", hoopCenterX, hoopCenterY, radius);

            return new Hoop(hoopCenterX, hoopCenterY, radius);

        } catch (Exception e) {
            LOG.error("Hoop detection failed in ROI", e);
        }

        return null;
    }

    public Hoop detectHoop(File frameFile) {

        try {

            BufferedImage img = ImageIO.read(frameFile);

            if (img == null) {
                LOG.warn("Image read failed");
                return null;
            }

            int width = img.getWidth();
            int height = img.getHeight();

            // analizziamo solo la parte alta
            int roiHeight = height / 3;

            int minX = width;
            int minY = roiHeight;
            int maxX = 0;
            int maxY = 0;

            int orangePixels = 0;

            for (int y = 0; y < roiHeight; y++) {

                for (int x = 0; x < width; x++) {

                    int rgb = img.getRGB(x, y);

                    int r = (rgb >> 16) & 0xff;
                    int g = (rgb >> 8) & 0xff;
                    int b = rgb & 0xff;

                    // filtro arancione (molto più robusto)
                    if (
                            r > 120 &&
                                    g > 60 &&
                                    b < 120 &&
                                    r > g &&
                                    g > b
                    ) {

                        orangePixels++;

                        if (x < minX) minX = x;
                        if (y < minY) minY = y;
                        if (x > maxX) maxX = x;
                        if (y > maxY) maxY = y;
                    }
                }
            }

            LOG.info("Orange pixels detected: " + orangePixels);

            if (orangePixels < 30) {
                LOG.warn("Not enough orange pixels");
                return null;
            }

            int centerX = (minX + maxX) / 2;
            int centerY = (minY + maxY) / 2;

            int radius = Math.max(maxX - minX, maxY - minY) / 2;

            LOG.infof("Hoop detected -> x=%d y=%d r=%d", centerX, centerY, radius);

            return new Hoop(centerX, centerY, radius);

        } catch (Exception e) {

            LOG.error("Hoop detection failed", e);
        }

        return null;
    }
}