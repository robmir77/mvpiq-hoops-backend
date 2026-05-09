package com.mvpiq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateExerciseRequestDTO {
    @NotBlank(message = "Title is required")
    private String title;
    
    private String category;
    
    private String difficulty;
    
    @Positive(message = "Duration must be positive")
    private Integer durationSeconds;
    
    private String instructions;
    
    @NotNull(message = "Owner ID is required")
    private UUID ownerId;
    
    private UUID mediaId;
}
