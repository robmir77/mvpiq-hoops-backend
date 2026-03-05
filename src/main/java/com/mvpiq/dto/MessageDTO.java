package com.mvpiq.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mvpiq.model.Message;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode
public class MessageDTO {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("conversation_id")
    private UUID conversationId;

    @JsonProperty("sender_id")
    private UUID senderId;

    @JsonProperty("message_text")
    private String messageText;

    @JsonProperty("media_id")
    private UUID mediaId;

    @JsonProperty("message_type")
    private String messageType; // text, media, system

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    // Factory method per convertire entity -> DTO
    public static MessageDTO fromEntity(Message message) {
        if (message == null) return null;
        return MessageDTO.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .messageText(message.getMessageText())
                .mediaId(message.getMediaId())
                .messageType(message.getMessageType())
                .createdAt(message.getCreatedAt())
                .build();
    }

    // Metodo opzionale per convertire DTO -> entity
    public Message toEntity() {
        Message message = new Message();
        message.setId(this.id != null ? this.id : UUID.randomUUID());
        message.setConversationId(this.conversationId);
        message.setSenderId(this.senderId);
        message.setMessageText(this.messageText);
        message.setMediaId(this.mediaId);
        message.setMessageType(this.messageType);
        message.setCreatedAt(this.createdAt != null ? this.createdAt : OffsetDateTime.now());
        return message;
    }
}
