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

    @Column(name = "user_id", columnDefinition = "UUID", nullable = false)
    @JsonProperty("user_id")
    private UUID userId;

    @Column(name = "program_id", columnDefinition = "UUID")
    @JsonProperty("program_id")
    private UUID programId;

    @Column(name = "session_date")
    @JsonProperty("session_date")
    private OffsetDateTime sessionDate = OffsetDateTime.now();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "session_data", columnDefinition = "jsonb")
    @JsonProperty("session_data")
    private String sessionData;

    @Column(name = "duration_seconds")
    @JsonProperty("duration_seconds")
    private Integer durationSeconds;

    @Column(name = "created_at")
    @JsonProperty("created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();
}