package com.mvpiq.dto;

import com.mvpiq.model.WorkoutFrameData;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class FrameDataResponse {
    private UUID id;
    private UUID sessionId;
    private Long frameTimestamp;
    private Double ballX;
    private Double ballY;
    private Double ballConfidence;
    private Double hoopX;
    private Double hoopY;
    private Double hoopConfidence;
    private Map<String, Object> poseData;
    private Map<String, Object> trajectoryData;
    private Double ballVelocityX;
    private Double ballVelocityY;
    private Boolean shotDetected;
    private OffsetDateTime createdAt;

    public static FrameDataResponse from(WorkoutFrameData f) {
        return FrameDataResponse.builder()
                .id(f.getId())
                .sessionId(f.getSession() != null ? f.getSession().getId() : null)
                .frameTimestamp(f.getFrameTimestamp())
                .ballX(f.getBallX())
                .ballY(f.getBallY())
                .ballConfidence(f.getBallConfidence())
                .hoopX(f.getHoopX())
                .hoopY(f.getHoopY())
                .hoopConfidence(f.getHoopConfidence())
                .poseData(f.getPoseData())
                .trajectoryData(f.getTrajectoryData())
                .ballVelocityX(f.getBallVelocityX())
                .ballVelocityY(f.getBallVelocityY())
                .shotDetected(f.getShotDetected())
                .createdAt(f.getCreatedAt())
                .build();
    }
}
