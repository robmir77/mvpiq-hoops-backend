package com.mvpiq.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PoseFrameDTO {

    // -------------------------
    // FRAME
    // -------------------------
    private int frameIndex;

    // -------------------------
    // UPPER BODY
    // -------------------------
    private KeyPointDTO leftShoulder;
    private KeyPointDTO rightShoulder;
    private KeyPointDTO leftElbow;
    private KeyPointDTO rightElbow;
    private KeyPointDTO leftWrist;
    private KeyPointDTO rightWrist;

    // -------------------------
    // LOWER BODY
    // -------------------------
    private KeyPointDTO leftHip;
    private KeyPointDTO rightHip;
    private KeyPointDTO leftKnee;
    private KeyPointDTO rightKnee;
    private KeyPointDTO leftAnkle;
    private KeyPointDTO rightAnkle;
}