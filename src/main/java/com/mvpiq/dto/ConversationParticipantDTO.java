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
public class ConversationParticipantDTO {
    private UUID id;
    private UUID conversationId;
    private UUID userId;
    private OffsetDateTime joinedAt;
    
    // Nested objects
    private UserDTO user;
    private ConversationDTO conversation;
}
