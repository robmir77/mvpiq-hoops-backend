package com.mvpiq.service;

import ai.djl.Application;
import ai.djl.ModelException;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.translator.YoloV5Translator;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;
import ai.djl.inference.Predictor;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import ai.djl.modality.cv.output.DetectedObjects;
import java.awt.Point;

public class BallTrackingAI {

    public List<Point> trackBallAI(List<File> frames) throws IOException, ModelException, TranslateException {

        // Definizione del modello pre-addestrato
        /*Criteria<Image, DetectedObjects> criteria = Criteria.builder()
                .optApplication(Application.CV.OBJECT_DETECTION)
                .setTypes(Image.class, DetectedObjects.class)
                .optFilter("backbone", "yolov5s") // modello leggero e veloce
                .optProgress(new ProgressBar())
                .build();*/

        Criteria<Image, DetectedObjects> criteria = Criteria.builder()
                .setTypes(Image.class, DetectedObjects.class)
                .optModelPath(Paths.get("C:/mvpiq-hoops/mvpiq-hoops-backend/target/model/yolov5s.pt"))
                .optEngine("PyTorch")
                .optTranslator(YoloV5Translator.builder().build())
                .optProgress(new ProgressBar())
                .build();

        List<Point> ballPositions = new ArrayList<>();

        try (ZooModel<Image, DetectedObjects> model = ModelZoo.loadModel(criteria);
             Predictor<Image, DetectedObjects> predictor = model.newPredictor()) {

            for (File frameFile : frames) {

                BufferedImage img = javax.imageio.ImageIO.read(frameFile);
                if (img == null) continue;

                Image image = ImageFactory.getInstance().fromImage(img);
                DetectedObjects detections = predictor.predict(image);

                for (int i = 0; i < detections.getNumberOfObjects(); i++) {
                    DetectedObjects.DetectedObject detectedObj = detections.item(i); // <- qui abbiamo il tipo corretto
                    if (detectedObj.getClassName().equalsIgnoreCase("sports ball")) {
                        ai.djl.modality.cv.output.BoundingBox box = detectedObj.getBoundingBox();
                        ai.djl.modality.cv.output.Rectangle rect = box.getBounds();

                        int cx = (int) (rect.getX() * img.getWidth() + rect.getWidth() * img.getWidth() / 2);
                        int cy = (int) (rect.getY() * img.getHeight() + rect.getHeight() * img.getHeight() / 2);

                        ballPositions.add(new Point(cx, cy));
                        System.out.println("Ball detected at: " + cx + "," + cy);
                    }
                }
            }
        }

        return ballPositions;
    }
}