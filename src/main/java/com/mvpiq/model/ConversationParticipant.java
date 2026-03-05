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
@Table(name = "conversation_participants", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"conversation_id", "user_id"})
})
public class ConversationParticipant {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id = UUID.randomUUID();

    @Column(name = "conversation_id", columnDefinition = "UUID", nullable = false)
    @JsonProperty("conversation_id")
    private UUID conversationId;

    @Column(name = "user_id", columnDefinition = "UUID", nullable = false)
    @JsonProperty("user_id")
    private UUID userId;

    @Column(name = "joined_at")
    @JsonProperty("joined_at")
    private OffsetDateTime joinedAt = OffsetDateTime.now();
}
