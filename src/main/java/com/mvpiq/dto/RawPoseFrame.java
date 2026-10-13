package com.mvpiq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawPoseFrame {
    
    private Long timestampMs;
    private List<PoseKeypoint> keypoints;
    private String coordinateSpace;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PoseKeypoint {
        private String name;
        private Double x;
        private Double y;
        private Double confidence;
    }
}
