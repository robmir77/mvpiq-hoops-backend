package com.mvpiq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FrameDataRequest {
    
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
}
