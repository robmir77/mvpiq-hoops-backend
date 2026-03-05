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
@Table(name = "exercises")
public class Exercise {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id = UUID.randomUUID();

    @Column(name = "media_id", columnDefinition = "UUID")
    @JsonProperty("media_id")
    private UUID mediaId;

    @Column(name = "creator_id", columnDefinition = "UUID")
    @JsonProperty("creator_id")
    private UUID creatorId;

    @Column(name = "title", length = 150, nullable = false)
    @JsonProperty("title")
    private String title;

    @Column(name = "category", length = 50)
    @JsonProperty("category")
    private String category;

    @Column(name = "difficulty", length = 30)
    @JsonProperty("difficulty")
    private String difficulty;

    @Column(name = "duration_minutes")
    @JsonProperty("duration_minutes")
    private Short durationMinutes;

    @Column(name = "instructions", columnDefinition = "TEXT")
    @JsonProperty("instructions")
    private String instructions;

    @Column(name = "created_at")
    @JsonProperty("created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at")
    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
