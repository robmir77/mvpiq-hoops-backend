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
@Table(name = "athlete_badges", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"athlete_id", "badge_id"})
})
public class AthleteBadge {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id = UUID.randomUUID();

    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerProfile player;

    @ManyToOne
    @JoinColumn(name = "badge_id", nullable = false)
    private Badge badge;

    @Column(name = "obtained_at")
    @JsonProperty("obtained_at")
    private OffsetDateTime obtainedAt = OffsetDateTime.now();
}
