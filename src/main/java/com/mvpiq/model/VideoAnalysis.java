package com.mvpiq.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
@Table(name = "video_analyses")
public class VideoAnalysis {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id = UUID.randomUUID();

    @Column(name = "media_id", columnDefinition = "UUID", nullable = false)
    @JsonProperty("media_id")
    private UUID mediaId;

    @Column(name = "analyzer_service", length = 100)
    @JsonProperty("analyzer_service")
    private String analyzerService;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "analysis_json", columnDefinition = "jsonb")
    @JsonProperty("analysis_json")
    private String analysisJson;

    @Column(name = "score", precision = 19, scale = 4)
    @JsonProperty("score")
    private BigDecimal score;

    @Column(name = "created_at")
    @JsonProperty("created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
