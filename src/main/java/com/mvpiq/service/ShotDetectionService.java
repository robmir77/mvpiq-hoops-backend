package com.mvpiq.service;

import ai.djl.Model;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.BoundingBox;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Rectangle;
import ai.djl.modality.cv.transform.ToTensor;
import ai.djl.modality.cv.translator.YoloV5Translator;
import ai.djl.translate.Translator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mvpiq.dto.ShotEventRequest;
import com.mvpiq.model.ShotEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ShotDetectionService {

    private static final Logger LOGGER = Logger.getLogger(ShotDetectionService.class.getName());

    @Inject
    ObjectMapper objectMapper;

    private final Map<String, Model> models = new ConcurrentHashMap<>();
    private final Map<String, Predictor<Image, DetectedObjects>> predictors = new ConcurrentHashMap<>();

    // Object detection classes for basketball
    private static final Set<String> TARGET_CLASSES = Set.of(
            "basketball", "ball", "hoop", "basket", "player", "person"
    );

    // Shot detection state tracking
    private static class TrackingState {
        boolean ballInHand = false;
        boolean shotInProgress = false;
        double lastBallX = 0;
        double lastBallY = 0;
        double lastBallVelocity = 0;
        long lastDetectionTime = 0;
        int consecutiveFramesWithBall = 0;
        int framesWithoutBall = 0;
    }

    private final Map<String, TrackingState> sessionTrackingStates = new ConcurrentHashMap<>();

    public void initializeModel(String modelPath, String modelName) throws IOException {
        try {
            Model model = Model.newInstance(modelName);
            
            // Configure YOLOv5 translator for basketball object detection
            Translator<Image, DetectedObjects> translator = YoloV5Translator.builder()
                    .build();
            
            model.load(Paths.get(modelPath));
            Predictor<Image, DetectedObjects> predictor = model.newPredictor(translator);
            
            models.put(modelName, model);
            predictors.put(modelName, predictor);
            
            LOGGER.info("Initialized shot detection model: " + modelName);
        } catch (Exception e) {
            LOGGER.error("Failed to initialize model: " + modelPath, e);
            throw new IOException("Failed to initialize shot detection model", e);
        }
    }

    public ShotDetectionResult detectObjects(byte[] imageData, String sessionId) throws IOException {
        try {
            Image image = ImageFactory.getInstance().fromInputStream(new ByteArrayInputStream(imageData));
            
            // Use default model if not specified
            String modelName = "yolov5-basketball";
            Predictor<Image, DetectedObjects> predictor = predictors.get(modelName);
            
            if (predictor == null) {
                throw new IllegalStateException("Model not initialized: " + modelName);
            }

            DetectedObjects detections = predictor.predict(image);
            
            // Process detections and extract basketball-specific objects
            List<BasketballObject> basketballObjects = new ArrayList<>();

            for (DetectedObjects.DetectedObject detection :
                    detections.<DetectedObjects.DetectedObject>items()) {

                if (TARGET_CLASSES.contains(detection.getClassName())) {
                    basketballObjects.add(new BasketballObject(
                            detection.getClassName(),
                            detection.getProbability(),
                            detection.getBoundingBox()
                    ));
                }
            }

            // Analyze for shot events
            ShotEventAnalysis shotAnalysis = analyzeForShotEvent(basketballObjects, sessionId);
            
            return new ShotDetectionResult(basketballObjects, shotAnalysis);
            
        } catch (Exception e) {
            LOGGER.error("Error during object detection", e);
            throw new IOException("Object detection failed", e);
        }
    }

    private ShotEventAnalysis analyzeForShotEvent(List<BasketballObject> objects, String sessionId) {
        TrackingState state = sessionTrackingStates.computeIfAbsent(sessionId, k -> new TrackingState());
        
        BasketballObject ball = findObject(objects, "basketball", "ball");
        BasketballObject hoop = findObject(objects, "hoop", "basket");
        BasketballObject player = findObject(objects, "player", "person");
        
        long currentTime = System.currentTimeMillis();
        
        // Shot detection logic based on specification
        ShotEventAnalysis analysis = new ShotEventAnalysis();
        analysis.setTimestamp(currentTime);
        
        if (ball != null && player != null) {
            Rectangle ballBounds = (Rectangle) ball.getBoundingBox();
            Rectangle playerBounds = (Rectangle) player.getBoundingBox();
            
            // Calculate ball position and velocity
            double ballX = ballBounds.getX() + ballBounds.getWidth() / 2;
            double ballY = ballBounds.getY() + ballBounds.getHeight() / 2;
            
            double velocity = calculateVelocity(ballX, ballY, state.lastBallX, state.lastBallY, 
                                               currentTime - state.lastDetectionTime);
            
            // Check if ball is near player's hands (simplified)
            boolean ballNearPlayer = isBallNearPlayer(ballBounds, playerBounds);
            
            // Shot detection conditions
            if (!state.shotInProgress && ballNearPlayer && velocity > 2.0) {
                // Potential shot initiation
                state.consecutiveFramesWithBall++;
                if (state.consecutiveFramesWithBall >= 3) {
                    state.ballInHand = true;
                }
            } else if (state.ballInHand && !ballNearPlayer) {
                // Ball separated from player - potential shot
                state.framesWithoutBall++;
                if (state.framesWithoutBall >= 2 && velocity > 3.0) {
                    state.shotInProgress = true;
                    analysis.setShotDetected(true);
                    analysis.setShotType("RELEASE");
                    analysis.setReleaseVelocity(velocity);
                    analysis.setReleaseAngle(calculateReleaseAngle(ballBounds, playerBounds));
                }
            } else if (state.shotInProgress && hoop != null) {
                // Check if shot reaches hoop area
                Rectangle hoopBounds = (Rectangle) hoop.getBoundingBox();
                double distanceToHoop = calculateDistance(ballX, ballY, 
                                                        hoopBounds.getX() + hoopBounds.getWidth() / 2,
                                                        hoopBounds.getY() + hoopBounds.getHeight() / 2);
                
                if (distanceToHoop < 50) { // Within hoop area
                    state.shotInProgress = false;
                    analysis.setShotResult(determineShotResult(ballBounds, hoopBounds, velocity));
                    analysis.setShotDetected(true);
                    analysis.setShotType("COMPLETED");
                }
            }
            
            // Update tracking state
            state.lastBallX = ballX;
            state.lastBallY = ballY;
            state.lastBallVelocity = velocity;
            state.lastDetectionTime = currentTime;
            
            if (!ballNearPlayer) {
                state.framesWithoutBall++;
                if (state.framesWithoutBall > 5) {
                    state.ballInHand = false;
                    state.consecutiveFramesWithBall = 0;
                    state.framesWithoutBall = 0;
                }
            }
        }
        
        return analysis;
    }

    private BasketballObject findObject(List<BasketballObject> objects, String... classNames) {
        return objects.stream()
                .filter(obj -> Arrays.asList(classNames).contains(obj.getClassName()))
                .max(Comparator.comparing(BasketballObject::getConfidence))
                .orElse(null);
    }

    private double calculateVelocity(double x1, double y1, double x2, double y2, long timeDelta) {
        if (timeDelta <= 0) return 0;
        double distance = Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
        return distance / (timeDelta / 1000.0); // pixels per second
    }

    private boolean isBallNearPlayer(Rectangle ballBounds, Rectangle playerBounds) {
        double ballCenterX = ballBounds.getX() + ballBounds.getWidth() / 2;
        double ballCenterY = ballBounds.getY() + ballBounds.getHeight() / 2;
        double playerCenterX = playerBounds.getX() + playerBounds.getWidth() / 2;
        double playerCenterY = playerBounds.getY() + playerBounds.getHeight() / 2;
        
        double distance = Math.sqrt(Math.pow(ballCenterX - playerCenterX, 2) + 
                                  Math.pow(ballCenterY - playerCenterY, 2));
        
        return distance < 100; // Threshold for "near" in pixels
    }

    private double calculateReleaseAngle(Rectangle ballBounds, Rectangle playerBounds) {
        double ballCenterY = ballBounds.getY() + ballBounds.getHeight() / 2;
        double playerCenterY = playerBounds.getY() + playerBounds.getHeight() / 2;
        
        // Simple angle calculation based on vertical position
        return Math.toDegrees(Math.atan2(playerCenterY - ballCenterY, 50)); // Assume 50px horizontal distance
    }

    private ShotEvent.ShotResult determineShotResult(Rectangle ballBounds, Rectangle hoopBounds, double velocity) {
        double ballCenterX = ballBounds.getX() + ballBounds.getWidth() / 2;
        double ballCenterY = ballBounds.getY() + ballBounds.getHeight() / 2;
        double hoopCenterX = hoopBounds.getX() + hoopBounds.getWidth() / 2;
        double hoopCenterY = hoopBounds.getY() + hoopBounds.getHeight() / 2;
        
        double distance = Math.sqrt(Math.pow(ballCenterX - hoopCenterX, 2) + 
                                  Math.pow(ballCenterY - hoopCenterY, 2));
        
        if (distance < 30) {
            return ShotEvent.ShotResult.MADE;
        } else if (velocity < 1.0) {
            return ShotEvent.ShotResult.AIRBALL;
        } else {
            return ShotEvent.ShotResult.MISS;
        }
    }

    private double calculateDistance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    public ShotEventRequest createShotEventFromDetection(ShotDetectionResult result, String sessionId) {
        ShotEventAnalysis analysis = result.getShotAnalysis();
        if (!analysis.isShotDetected()) {
            return null;
        }

        return ShotEventRequest.builder()
                .timestampMs(analysis.getTimestamp())
                .shotResult(analysis.getShotResult())
                .releaseAngle(analysis.getReleaseAngle())
                .releaseVelocity(analysis.getReleaseVelocity())
                .detectionConfidence(calculateOverallConfidence(result.getObjects()))
                .trackingData(serializeTrackingData(result))
                .build();
    }

    private Double calculateOverallConfidence(List<BasketballObject> objects) {
        if (objects.isEmpty()) return 0.0;
        return objects.stream()
                .mapToDouble(BasketballObject::getConfidence)
                .average()
                .orElse(0.0);
    }

    private String serializeTrackingData(ShotDetectionResult result) {
        try {
            Map<String, Object> trackingData = new HashMap<>();
            trackingData.put("objects", result.getObjects());
            trackingData.put("analysis", result.getShotAnalysis());
            return objectMapper.writeValueAsString(trackingData);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to serialize tracking data", e);
            return "{}";
        }
    }

    public void clearTrackingState(String sessionId) {
        sessionTrackingStates.remove(sessionId);
    }

    // Data classes for detection results
    public static class ShotDetectionResult {
        private final List<BasketballObject> objects;
        private final ShotEventAnalysis shotAnalysis;

        public ShotDetectionResult(List<BasketballObject> objects, ShotEventAnalysis shotAnalysis) {
            this.objects = objects;
            this.shotAnalysis = shotAnalysis;
        }

        public List<BasketballObject> getObjects() { return objects; }
        public ShotEventAnalysis getShotAnalysis() { return shotAnalysis; }
    }

    public static class BasketballObject {
        private final String className;
        private final double confidence;
        private final BoundingBox boundingBox;

        public BasketballObject(String className, double confidence, BoundingBox boundingBox) {
            this.className = className;
            this.confidence = confidence;
            this.boundingBox = boundingBox;
        }

        public String getClassName() { return className; }
        public double getConfidence() { return confidence; }
        public BoundingBox getBoundingBox() { return boundingBox; }
    }

    public static class ShotEventAnalysis {
        private long timestamp;
        private boolean shotDetected;
        private String shotType; // RELEASE, COMPLETED
        private ShotEvent.ShotResult shotResult;
        private double releaseAngle;
        private double releaseVelocity;

        // Getters and setters
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

        public boolean isShotDetected() { return shotDetected; }
        public void setShotDetected(boolean shotDetected) { this.shotDetected = shotDetected; }

        public String getShotType() { return shotType; }
        public void setShotType(String shotType) { this.shotType = shotType; }

        public ShotEvent.ShotResult getShotResult() { return shotResult; }
        public void setShotResult(ShotEvent.ShotResult shotResult) { this.shotResult = shotResult; }

        public double getReleaseAngle() { return releaseAngle; }
        public void setReleaseAngle(double releaseAngle) { this.releaseAngle = releaseAngle; }

        public double getReleaseVelocity() { return releaseVelocity; }
        public void setReleaseVelocity(double releaseVelocity) { this.releaseVelocity = releaseVelocity; }
    }
}
