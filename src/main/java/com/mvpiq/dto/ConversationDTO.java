package com.mvpiq.dto;

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
public class ConversationDTO {
    private UUID id;
    private String title;
    private OffsetDateTime createdAt;
    
    // Nested objects
    private List<ConversationParticipantDTO> participants;
    private List<MessageDTO> messages;
    private Integer unreadCount;
}
