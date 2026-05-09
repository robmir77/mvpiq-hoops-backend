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

    @ManyToOne
    @JoinColumn(name = "media_id")
    private MediaAsset media;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(name = "title", length = 150, nullable = false)
    @JsonProperty("title")
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    @JsonProperty("description")
    private String description;

    @Column(name = "category", length = 50)
    @JsonProperty("category")
    private String category;

    @Column(name = "difficulty", length = 30)
    @JsonProperty("difficulty")
    private String difficulty;

    @Column(name = "duration_seconds")
    @JsonProperty("duration_seconds")
    private Integer durationSeconds;

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
