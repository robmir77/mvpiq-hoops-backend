package com.mvpiq.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
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
@Table(name = "athlete_goals")
public class AthleteGoal {

    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id = UUID.randomUUID();

    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerProfile player;

    @Column(name = "title", length = 200, nullable = false)
    @JsonProperty("title")
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    @JsonProperty("description")
    private String description;

    @Column(name = "target_value", precision = 19, scale = 2)
    @JsonProperty("target_value")
    private BigDecimal targetValue;

    @Column(name = "current_value", precision = 19, scale = 2)
    @JsonProperty("current_value")
    private BigDecimal currentValue = BigDecimal.ZERO;

    @Column(name = "unit", length = 50)
    @JsonProperty("unit")
    private String unit;

    @Column(name = "created_at")
    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @Column(name = "due_date")
    @JsonProperty("due_date")
    private LocalDate dueDate;

    @Column(name = "completed_at")
    @JsonProperty("completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "status", length = 20, nullable = false)
    @JsonProperty("status")
    private String status = "ACTIVE";

    // Metodo helper per ottenere l'ID dell'atleta
    public UUID getAthleteId() {
        return player != null ? player.getId() : null;
    }

    // Metodo helper per il builder
    public static class AthleteGoalBuilder {
        private PlayerProfile player;

        public AthleteGoalBuilder athleteId(UUID athleteId) {
            // Questo metodo è solo per compatibilità - non fa nulla reale
            // L'ID del player deve essere impostato tramite il campo player
            return this;
        }

        public AthleteGoalBuilder player(PlayerProfile player) {
            this.player = player;
            return this;
        }
    }
}
