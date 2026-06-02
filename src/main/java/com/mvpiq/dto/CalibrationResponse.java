package com.mvpiq.dto;

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
public class CalibrationResponse {

    private UUID id;
    private Double hoopCenterX;
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
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static CalibrationResponse from(com.mvpiq.model.CourtCalibration calibration) {
        return CalibrationResponse.builder()
                .id(calibration.getId())
                .hoopCenterX(calibration.getHoopCenterX())
                .hoopCenterY(calibration.getHoopCenterY())
                .freeThrowLineX(calibration.getFreeThrowLineX())
                .freeThrowLineY(calibration.getFreeThrowLineY())
                .threePointLineTopX(calibration.getThreePointLineTopX())
                .threePointLineTopY(calibration.getThreePointLineTopY())
                .threePointLineLeftX(calibration.getThreePointLineLeftX())
                .threePointLineLeftY(calibration.getThreePointLineLeftY())
                .threePointLineRightX(calibration.getThreePointLineRightX())
                .threePointLineRightY(calibration.getThreePointLineRightY())
                .baselineX(calibration.getBaselineX())
                .baselineY(calibration.getBaselineY())
                .sidelineLeftX(calibration.getSidelineLeftX())
                .sidelineLeftY(calibration.getSidelineLeftY())
                .sidelineRightX(calibration.getSidelineRightX())
                .sidelineRightY(calibration.getSidelineRightY())
                .homographyMatrix(calibration.getHomographyMatrix())
                .calibrationConfidence(calibration.getCalibrationConfidence())
                .createdAt(calibration.getCreatedAt())
                .updatedAt(calibration.getUpdatedAt())
                .build();
    }
}
