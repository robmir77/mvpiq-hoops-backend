package com.mvpiq.dto;

import com.mvpiq.model.ShotEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShotEventResponse {
    
    private UUID id;
    private UUID sessionId;
    private Long timestampMs;
    private ShotEvent.ShotResult shotResult;
    private Double courtX;
    private Double courtY;
    private Double distanceFromHoop;
    private Double releaseAngle;
    private Double releaseVelocity;
    private Double shotArcHeight;
    private Long videoTimestampMs;
    private Double detectionConfidence;
    private String trackingData;
    private String videoClipPath;
    private OffsetDateTime createdAt;
    
    public static ShotEventResponse from(com.mvpiq.model.ShotEvent shot) {
        return ShotEventResponse.builder()
                .id(shot.getId())
                .sessionId(shot.getWorkoutSession() != null ? shot.getWorkoutSession().getId() : null)
                .timestampMs(shot.getTimestampMs())
                .shotResult(shot.getShotResult())
                .courtX(shot.getCourtX())
                .courtY(shot.getCourtY())
                .distanceFromHoop(shot.getDistanceFromHoop())
                .releaseAngle(shot.getReleaseAngle())
                .releaseVelocity(shot.getReleaseVelocity())
                .shotArcHeight(shot.getShotArcHeight())
                .videoTimestampMs(shot.getVideoTimestampMs())
                .detectionConfidence(shot.getDetectionConfidence())
                .trackingData(shot.getTrackingData())
                .videoClipPath(shot.getVideoClipPath())
                .createdAt(shot.getCreatedAt())
                .build();
    }
}
