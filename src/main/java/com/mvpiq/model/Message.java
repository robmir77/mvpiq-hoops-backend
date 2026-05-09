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

    @ManyToOne
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private User sender;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @ManyToOne
    @JoinColumn(name = "media_id")
    private MediaAsset media;

    @Column(name = "message_type", length = 20)
    private String messageType = "text";

    @Column(name = "created_at")
    @JsonProperty("created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
