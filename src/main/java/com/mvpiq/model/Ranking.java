package com.mvpiq.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
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
@Table(name = "rankings", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"player_id", "rank_scope", "scope_value", "season_year"})
})
public class Ranking {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id = UUID.randomUUID();

    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerProfile player;

    @Column(name = "scope", length = 50, nullable = false)
    @JsonProperty("scope")
    private String scope;

    @Column(name = "scope_value", length = 200)
    @JsonProperty("scope_value")
    private String scopeValue;

    @Column(name = "rank_position")
    @JsonProperty("rank_position")
    private Integer rankPosition;

    @Column(name = "score", precision = 19, scale = 4)
    @JsonProperty("score")
    private BigDecimal score;

    @Column(name = "season_year")
    @JsonProperty("season_year")
    private Integer seasonYear;

    @Column(name = "created_at")
    @JsonProperty("created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
