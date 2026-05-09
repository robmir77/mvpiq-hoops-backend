package com.mvpiq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateConversationRequestDTO {
    private String title;
    
    @NotEmpty(message = "At least one participant is required")
    private List<UUID> participantIds;
}
