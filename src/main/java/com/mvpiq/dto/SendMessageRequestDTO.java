package com.mvpiq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequestDTO {
    @NotNull(message = "Sender ID is required")
    private UUID senderId;
    
    @NotBlank(message = "Message content is required")
    private String content;
    
    private String messageType;
    
    private UUID mediaId;
}
