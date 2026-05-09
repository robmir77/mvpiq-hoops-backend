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
public class TrainerFollowDTO {
    private UUID id;
    private UUID trainerId;
    private UUID playerId;
    private OffsetDateTime createdAt;
    
    // Nested objects
    private UserDTO trainer;
    private PlayerProfileDTO player;
}
