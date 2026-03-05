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

    @Column(name = "athlete_id", columnDefinition = "UUID", nullable = false)
    @JsonProperty("athlete_id")
    private UUID athleteId;

    @Column(name = "badge_id", columnDefinition = "UUID", nullable = false)
    @JsonProperty("badge_id")
    private UUID badgeId;

    @Column(name = "obtained_at")
    @JsonProperty("obtained_at")
    private OffsetDateTime obtainedAt = OffsetDateTime.now();
}
