package com.mvpiq.dto;

import com.mvpiq.model.ShotEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShotEventRequest {
    
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
}
