package com.mvpiq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoutSavedFilterDTO {
    private UUID id;
    private UUID scoutId;
    private String name;
    private Map<String, Object> filterJson;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    
    // Nested object
    private UserDTO scout;
}
