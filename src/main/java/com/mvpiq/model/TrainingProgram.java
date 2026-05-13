package com.mvpiq.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mvpiq.enums.GenerationStatus;
import com.mvpiq.enums.SourceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
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
@Table(name = "training_programs")
public class TrainingProgram {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(name = "title", length = 200, nullable = false)
    @JsonProperty("title")
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    @JsonProperty("description")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "program_json", columnDefinition = "jsonb")
    @JsonProperty("program_json")
    private Object programJson;

    @Column(name = "is_public")
    @JsonProperty("is_public")
    private Boolean isPublic = true;

    @Column(name = "created_at")
    @JsonProperty("created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at")
    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @Column(name = "estimated_duration_minutes")
    @JsonProperty("estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    @Column(name = "difficulty", length = 20)
    @JsonProperty("difficulty")
    private String difficulty;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "jsonb")
    @JsonProperty("tags")
    private Object tags;

    @Column(name = "published_at")
    @JsonProperty("published_at")
    private OffsetDateTime publishedAt;

    // AI-related fields
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    @JsonProperty("source_type")
    private SourceType sourceType = SourceType.MANUAL;

    @ManyToOne
    @JoinColumn(name = "goal_id")
    @JsonProperty("goal_id")
    private AthleteGoal goal;

    @Column(name = "generated_by_ai", nullable = false)
    @JsonProperty("generated_by_ai")
    private Boolean generatedByAi = false;

    @Column(name = "ai_model", length = 100)
    @JsonProperty("ai_model")
    private String aiModel;

    @Column(name = "ai_prompt", columnDefinition = "TEXT")
    @JsonProperty("ai_prompt")
    private String aiPrompt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_generation_parameters", columnDefinition = "jsonb")
    @JsonProperty("ai_generation_parameters")
    private Object aiGenerationParameters;

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_status", nullable = false, length = 20)
    @JsonProperty("generation_status")
    private GenerationStatus generationStatus = GenerationStatus.COMPLETED;

    @Column(name = "generated_at")
    @JsonProperty("generated_at")
    private OffsetDateTime generatedAt;

    @ManyToOne
    @JoinColumn(name = "parent_program_id")
    @JsonProperty("parent_program_id")
    private TrainingProgram parentProgram;
}
