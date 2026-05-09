package com.mvpiq.dto.ai;

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
public class TrainingProgramDTO {
    
    private UUID id;
    private UUID athleteId;
    private String title;
    private String description;
    private String sourceType;
    private UUID goalId;
    private Boolean generatedByAi;
    private String aiModel;
    private String generationStatus;
    private OffsetDateTime createdAt;
    private OffsetDateTime generatedAt;
    private List<TrainingWeekDTO> weeks;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrainingWeekDTO {
        private Integer week;
        private List<TrainingDayDTO> days;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrainingDayDTO {
        private Integer day;
        private String title;
        private List<TrainingExerciseDTO> exercises;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrainingExerciseDTO {
        private String exerciseId;
        private String name;
        private Integer durationMinutes;
        private Integer repetitions;
        private String notes;
    }
}
