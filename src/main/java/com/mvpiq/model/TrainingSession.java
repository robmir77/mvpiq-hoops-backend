package com.mvpiq.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
@Table(name = "training_sessions")
public class TrainingSession {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id = UUID.randomUUID();

    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    private User player;

    @ManyToOne
    @JoinColumn(name = "program_id")
    private TrainingProgram program;

    @Column(name = "session_date")
    @JsonProperty("session_date")
    private OffsetDateTime sessionDate = OffsetDateTime.now();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "session_data", columnDefinition = "jsonb")
    @JsonProperty("session_data")
    private Object sessionData;

    @Column(name = "duration_seconds")
    @JsonProperty("duration_seconds")
    private Integer durationSeconds;

    @Column(name = "calories_burned")
    @JsonProperty("calories_burned")
    private Integer caloriesBurned;

    @Column(name = "average_heart_rate")
    @JsonProperty("average_heart_rate")
    private Integer averageHeartRate;

    @Column(name = "perceived_effort")
    @JsonProperty("perceived_effort")
    private Short perceivedEffort;

    @Column(name = "created_at")
    @JsonProperty("created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();
}