package com.mvpiq.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode
public class AthleteGoalDTO {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("athlete_id")
    private UUID athleteId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    @JsonProperty("target_value")
    private BigDecimal targetValue;

    @JsonProperty("current_value")
    private BigDecimal currentValue;

    @JsonProperty("unit")
    private String unit;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("due_date")
    private LocalDate dueDate;

    @JsonProperty("completed_at")
    private OffsetDateTime completedAt;

    @JsonProperty("status")
    private String status; // ACTIVE | COMPLETED | CANCELLED
}
