package com.mvpiq.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ShotFrameAnalysisDTO {
    private int frameIndex;
    private BallPointDTO ball;
    private PoseFrameDTO pose;
    private double ballRightWristDistance;
    private double ballLeftWristDistance;
    private Double rightElbowAngle;
    private Double leftElbowAngle;
    private boolean candidateRelease;
}
