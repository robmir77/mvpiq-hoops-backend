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
@Table(name = "trainer_follows", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"trainer_id", "player_id"})
})
public class TrainerFollow {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id = UUID.randomUUID();

    @Column(name = "trainer_id", columnDefinition = "UUID", nullable = false)
    @JsonProperty("trainer_id")
    private UUID trainerId;

    @Column(name = "player_id", columnDefinition = "UUID", nullable = false)
    @JsonProperty("player_id")
    private UUID playerId;

    @Column(name = "created_at")
    @JsonProperty("created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
