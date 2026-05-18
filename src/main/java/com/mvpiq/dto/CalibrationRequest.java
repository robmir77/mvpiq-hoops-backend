package com.mvpiq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalibrationRequest {

    @NotNull
    private Double hoopCenterX;

    @NotNull
    private Double hoopCenterY;

    private Double freeThrowLineX;

    private Double freeThrowLineY;

    private Double threePointLineTopX;

    private Double threePointLineTopY;

    private Double threePointLineLeftX;

    private Double threePointLineLeftY;

    private Double threePointLineRightX;

    private Double threePointLineRightY;

    private Double baselineX;

    private Double baselineY;

    private Double sidelineLeftX;

    private Double sidelineLeftY;

    private Double sidelineRightX;

    private Double sidelineRightY;

    private List<Double> homographyMatrix;

    private Double calibrationConfidence;
}
