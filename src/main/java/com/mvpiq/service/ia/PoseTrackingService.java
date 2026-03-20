package com.mvpiq.service.ia;

import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.util.NDImageUtils;
import ai.djl.ndarray.*;
import ai.djl.ndarray.types.DataType;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;

import com.mvpiq.dto.BallPointDTO;
import com.mvpiq.dto.KeyPointDTO;
import com.mvpiq.dto.PoseFrameDTO;
import com.mvpiq.enums.HandSide;

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
public class PoseTrackingService {

    private static final Logger LOG = Logger.getLogger(PoseTrackingService.class);

    private ZooModel<NDList, NDList> model;
    private Predictor<NDList, NDList> predictor;

    private NDManager manager;

    // -------------------------------------------------------
    // INIT
    // -------------------------------------------------------

    @PostConstruct
    void init() {

        LOG.info("🧍 Loading MoveNet pose model...");

        try {

            manager = NDManager.newBaseManager();

            Criteria<NDList, NDList> criteria =
                    Criteria.builder()
                            .setTypes(NDList.class, NDList.class)
                            .optModelPath(Paths.get("models/model.onnx"))
                            .optEngine("OnnxRuntime")
                            .optProgress(new ProgressBar())
                            .build();

            model = ModelZoo.loadModel(criteria);
            predictor = model.newPredictor();

            LOG.info("✅ Pose model loaded");

        } catch (Exception e) {

            LOG.error("❌ Failed to load pose model", e);

        }
    }

    // -------------------------------------------------------
    // MAIN POSE TRACKING
    // -------------------------------------------------------

    public List<PoseFrameDTO> trackPose(List<File> frames) {

        List<PoseFrameDTO> result = new ArrayList<>();

        if (frames == null || frames.isEmpty()) {
            LOG.warn("No frames received for pose tracking");
            return result;
        }

        int frameIndex = 0;

        for (File frameFile : frames) {

            frameIndex++;

            try {

                BufferedImage img = ImageIO.read(frameFile);

                if (img == null) {
                    LOG.warnf("⚠️ Null image for frame %d", frameIndex);
                    continue;
                }

                PoseFrameDTO pose = detectPose(img, frameIndex);

                if (pose != null) {
                    result.add(pose);
                }

            } catch (Exception e) {

                LOG.errorf(e, "❌ Failed pose tracking for frame %d", frameIndex);

            }
        }

        LOG.infof("✅ Pose tracking completed. Frames processed: %d", result.size());

        return result;
    }

    // -------------------------------------------------------
    // POSE INFERENCE
    // -------------------------------------------------------

    private PoseFrameDTO detectPose(BufferedImage img, int frameIndex)
            throws TranslateException {

        PoseFrameDTO dto = new PoseFrameDTO();
        dto.setFrameIndex(frameIndex);

        if (predictor == null) {
            LOG.warn("Pose predictor not available");
            return dto;
        }

        Image djlImage = ImageFactory.getInstance().fromImage(img);

        // resize richiesto da MoveNet
        NDArray array = djlImage.toNDArray(manager);

        // Resize corretto richiesto da MoveNet (256x256)
        array = NDImageUtils.resize(array, 256, 256);

        // Converti in float32
        array = array.toType(DataType.INT32, false);

        // Normalizzazione tipica MoveNet: [0,1]
        //array = array.div(255.0f);

        // Transpose HWC -> CHW
        //array = array.transpose(2, 0, 1);

        // Aggiungi dimensione batch
        array = array.expandDims(0);

        NDList input = new NDList(array);

        NDList output = predictor.predict(input);

        float[] data = output.singletonOrThrow().toFloatArray();

        int width = img.getWidth();
        int height = img.getHeight();

        dto.setLeftShoulder(map(data,5,width,height));
        dto.setRightShoulder(map(data,6,width,height));

        dto.setLeftElbow(map(data,7,width,height));
        dto.setRightElbow(map(data,8,width,height));

        dto.setLeftWrist(map(data,9,width,height));
        dto.setRightWrist(map(data,10,width,height));

        return dto;
    }

    // -------------------------------------------------------
    // KEYPOINT MAPPING
    // -------------------------------------------------------

    private KeyPointDTO map(float[] data,int index,int width,int height){

        int base=index*3;

        float y=data[base];
        float x=data[base+1];
        float confidence=data[base+2];

        KeyPointDTO p=new KeyPointDTO();

        p.setX(x*width);
        p.setY(y*height);
        p.setConfidence(confidence);

        return p;
    }

    // -------------------------------------------------------
    // SHOOTING HAND DETECTION
    // -------------------------------------------------------

    public HandSide detectShootingHand(BallPointDTO ball, PoseFrameDTO pose) {

        if (ball == null || pose == null) {
            return HandSide.UNKNOWN;
        }

        KeyPointDTO leftWrist = pose.getLeftWrist();
        KeyPointDTO rightWrist = pose.getRightWrist();

        boolean leftValid = leftWrist != null && leftWrist.isValid(0.3);
        boolean rightValid = rightWrist != null && rightWrist.isValid(0.3);

        if (!leftValid && !rightValid) {
            return HandSide.UNKNOWN;
        }

        if (leftValid && !rightValid) {
            return HandSide.LEFT;
        }

        if (!leftValid) {
            return HandSide.RIGHT;
        }

        double leftDist = PoseMathUtils.distance(leftWrist, ball);
        double rightDist = PoseMathUtils.distance(rightWrist, ball);

        return rightDist < leftDist ? HandSide.RIGHT : HandSide.LEFT;
    }

    // -------------------------------------------------------
    // SHOOTING HAND ESTIMATION
    // -------------------------------------------------------
    public HandSide estimateShootingHand(ShotContext ctx) {

        List<BallPointDTO> ballPoints = ctx.ballNorm;
        List<PoseFrameDTO> poses = ctx.poseFrames;

        if (ballPoints == null || poses == null || ballPoints.isEmpty() || poses.isEmpty()) {
            return HandSide.UNKNOWN;
        }

        int from = Math.max(0, ctx.releaseFrame - 3);
        int to = ctx.releaseFrame;

        int leftCount = 0;
        int rightCount = 0;

        int max = Math.min(ballPoints.size(), poses.size());

        for (int i = from; i <= to && i < max; i++) {

            HandSide side = detectShootingHand(ballPoints.get(i), poses.get(i));

            if (side == HandSide.LEFT) leftCount++;
            else if (side == HandSide.RIGHT) rightCount++;
        }

        if (leftCount == 0 && rightCount == 0) {
            return HandSide.UNKNOWN;
        }

        return rightCount >= leftCount ? HandSide.RIGHT : HandSide.LEFT;
    }

    // -------------------------------------------------------
    // KEYPOINT HELPERS
    // -------------------------------------------------------

    public KeyPointDTO getWrist(PoseFrameDTO pose, HandSide side) {

        if (pose == null || side == null) {
            return null;
        }

        return switch (side) {
            case LEFT -> pose.getLeftWrist();
            case RIGHT -> pose.getRightWrist();
            default -> null;
        };
    }

    public KeyPointDTO getElbow(PoseFrameDTO pose, HandSide side) {

        if (pose == null || side == null) {
            return null;
        }

        return switch (side) {
            case LEFT -> pose.getLeftElbow();
            case RIGHT -> pose.getRightElbow();
            default -> null;
        };
    }

    public KeyPointDTO getShoulder(PoseFrameDTO pose, HandSide side) {

        if (pose == null || side == null) {
            return null;
        }

        return switch (side) {
            case LEFT -> pose.getLeftShoulder();
            case RIGHT -> pose.getRightShoulder();
            default -> null;
        };
    }
}