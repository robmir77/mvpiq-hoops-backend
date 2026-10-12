package com.mvpiq.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mvpiq.dto.ShotEventResponse;
import com.mvpiq.model.WorkoutSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutSessionResponse {

    // Campo principale usato dal FE
    private UUID id;
    private UUID playerId;
    private WorkoutSession.CameraMode cameraMode;
    private WorkoutSession.CourtType courtType;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private Integer totalShots;
    private Integer madeShots;
    // "status" usato dal FE, "sessionStatus" mantenuto per retrocompatibilità
    private String status;
    @JsonProperty("sessionStatus")
    private String sessionStatus;
    private Double shootingPercentage;
    private List<ShotEventResponse> recentShots;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Integer fps;
    private Integer cameraResolutionWidth;
    private Integer cameraResolutionHeight;
    private String poseResolution;
    private String yoloModelId;
    private String moveNetModelId;
    private String yoloDelegate;
    private String poseDelegate;
    private CalibrationResponse calibration;

    public static WorkoutSessionResponse from(com.mvpiq.model.WorkoutSession session) {
        return from(session, null);
    }

    public static WorkoutSessionResponse from(com.mvpiq.model.WorkoutSession session, com.mvpiq.model.CourtCalibration calibration) {
        Double percentage = null;
        if (session.getTotalShots() != null && session.getTotalShots() > 0) {
            percentage = (session.getMadeShots() != null ? session.getMadeShots() : 0) * 100.0 / session.getTotalShots();
        }

        CalibrationResponse calibrationResponse = null;
        if (calibration != null) {
            calibrationResponse = CalibrationResponse.from(calibration);
        }

        return WorkoutSessionResponse.builder()
                .id(session.getId())
                .playerId(session.getPlayer() != null ? session.getPlayer().getId() : null)
                .cameraMode(session.getCameraMode())
                .courtType(session.getCourtType())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .totalShots(session.getTotalShots())
                .madeShots(session.getMadeShots())
                .status(session.getSessionStatus())
                .sessionStatus(session.getSessionStatus())
                .shootingPercentage(percentage)
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .fps(session.getFps())
                .cameraResolutionWidth(session.getCameraResolutionWidth())
                .cameraResolutionHeight(session.getCameraResolutionHeight())
                .poseResolution(session.getPoseResolution())
                .yoloModelId(session.getYoloModelId())
                .moveNetModelId(session.getMoveNetModelId())
                .yoloDelegate(session.getYoloDelegate())
                .poseDelegate(session.getPoseDelegate())
                .calibration(calibrationResponse)
                .build();
    }
}
