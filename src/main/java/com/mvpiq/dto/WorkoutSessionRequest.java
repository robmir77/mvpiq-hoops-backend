package com.mvpiq.dto;

import com.mvpiq.model.WorkoutSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutSessionRequest {
    
    private WorkoutSession.CameraMode cameraMode;
    private WorkoutSession.CourtType courtType;
    private String calibrationData;
    
    public static class CalibrationData {
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
        private String homographyMatrix;
        
        // Getters and setters
        public Double getHoopCenterX() { return hoopCenterX; }
        public void setHoopCenterX(Double hoopCenterX) { this.hoopCenterX = hoopCenterX; }
        
        public Double getHoopCenterY() { return hoopCenterY; }
        public void setHoopCenterY(Double hoopCenterY) { this.hoopCenterY = hoopCenterY; }
        
        public Double getFreeThrowLineX() { return freeThrowLineX; }
        public void setFreeThrowLineX(Double freeThrowLineX) { this.freeThrowLineX = freeThrowLineX; }
        
        public Double getFreeThrowLineY() { return freeThrowLineY; }
        public void setFreeThrowLineY(Double freeThrowLineY) { this.freeThrowLineY = freeThrowLineY; }
        
        public Double getThreePointLineTopX() { return threePointLineTopX; }
        public void setThreePointLineTopX(Double threePointLineTopX) { this.threePointLineTopX = threePointLineTopX; }
        
        public Double getThreePointLineTopY() { return threePointLineTopY; }
        public void setThreePointLineTopY(Double threePointLineTopY) { this.threePointLineTopY = threePointLineTopY; }
        
        public Double getThreePointLineLeftX() { return threePointLineLeftX; }
        public void setThreePointLineLeftX(Double threePointLineLeftX) { this.threePointLineLeftX = threePointLineLeftX; }
        
        public Double getThreePointLineLeftY() { return threePointLineLeftY; }
        public void setThreePointLineLeftY(Double threePointLineLeftY) { this.threePointLineLeftY = threePointLineLeftY; }
        
        public Double getThreePointLineRightX() { return threePointLineRightX; }
        public void setThreePointLineRightX(Double threePointLineRightX) { this.threePointLineRightX = threePointLineRightX; }
        
        public Double getThreePointLineRightY() { return threePointLineRightY; }
        public void setThreePointLineRightY(Double threePointLineRightY) { this.threePointLineRightY = threePointLineRightY; }
        
        public String getHomographyMatrix() { return homographyMatrix; }
        public void setHomographyMatrix(String homographyMatrix) { this.homographyMatrix = homographyMatrix; }
    }
}
