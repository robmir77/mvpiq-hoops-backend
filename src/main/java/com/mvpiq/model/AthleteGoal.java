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

    @Column(name = "athlete_id", columnDefinition = "UUID", nullable = false)
    @JsonProperty("athlete_id")
    private UUID athleteId;

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
}
