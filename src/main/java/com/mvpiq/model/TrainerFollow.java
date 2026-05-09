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

    @ManyToOne
    @JoinColumn(name = "trainer_id", nullable = false)
    private User trainer;

    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerProfile player;

    @Column(name = "created_at")
    @JsonProperty("created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
