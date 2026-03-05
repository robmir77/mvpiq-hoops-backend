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
@Table(name = "athlete_points")
public class AthletePoints {

    @Id
    @Column(name = "athlete_id", columnDefinition = "UUID")
    private UUID athleteId;

    @Column(name = "total_points")
    @JsonProperty("total_points")
    private Long totalPoints = 0L;

    @Column(name = "updated_at")
    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
