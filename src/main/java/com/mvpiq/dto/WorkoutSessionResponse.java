package com.mvpiq.dto;

import com.mvpiq.model.WorkoutSession;
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
public class WorkoutSessionResponse {
    
    private UUID sessionId;
    private UUID playerId;
    private WorkoutSession.CameraMode cameraMode;
    private WorkoutSession.CourtType courtType;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private Integer totalShots;
    private Integer madeShots;
    private String sessionStatus;
    private Double shootingPercentage;
    private List<ShotEventResponse> recentShots;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    
    public static WorkoutSessionResponse from(com.mvpiq.model.WorkoutSession session) {
        Double percentage = null;
        if (session.getTotalShots() != null && session.getTotalShots() > 0) {
            percentage = (session.getMadeShots() != null ? session.getMadeShots() : 0) * 100.0 / session.getTotalShots();
        }
        
        return WorkoutSessionResponse.builder()
                .sessionId(session.getId())
                .playerId(session.getPlayer() != null ? session.getPlayer().getId() : null)
                .cameraMode(session.getCameraMode())
                .courtType(session.getCourtType())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .totalShots(session.getTotalShots())
                .madeShots(session.getMadeShots())
                .sessionStatus(session.getSessionStatus())
                .shootingPercentage(percentage)
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }
}
