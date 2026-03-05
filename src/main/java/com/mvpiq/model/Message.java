package com.mvpiq.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder
@Entity
@Table(name = "messages")
public class Message {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id = UUID.randomUUID();

    @Column(name = "conversation_id", columnDefinition = "UUID", nullable = false)
    @JsonProperty("conversation_id")
    private UUID conversationId;

    @Column(name = "sender_id", columnDefinition = "UUID")
    @JsonProperty("sender_id")
    private UUID senderId;

    @Column(name = "message_text", columnDefinition = "TEXT")
    @JsonProperty("message_text")
    private String messageText;

    @Column(name = "media_id", columnDefinition = "UUID")
    @JsonProperty("media_id")
    private UUID mediaId;

    @Column(name = "message_type", length = 20)
    @JsonProperty("message_type")
    private String messageType = "text";

    @Column(name = "created_at")
    @JsonProperty("created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
