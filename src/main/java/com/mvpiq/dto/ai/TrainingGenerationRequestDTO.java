package com.mvpiq.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingGenerationRequestDTO {
    
    private UUID athleteId;
    private String goal;
    private String position;
    private String skillLevel;
    private Integer sessionsPerWeek;
    private Integer sessionDurationMinutes;
    private Integer weeks;
    private String additionalNotes;
}
