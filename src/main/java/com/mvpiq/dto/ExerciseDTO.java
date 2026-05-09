package com.mvpiq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseDTO {
    private UUID id;
    private UUID ownerId;
    private UUID mediaId;
    private String title;
    private String category;
    private String difficulty;
    private Integer durationSeconds;
    private String instructions;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    
    // Nested objects for full response
    private UserDTO owner;
    private MediaAssetDTO media;
}
