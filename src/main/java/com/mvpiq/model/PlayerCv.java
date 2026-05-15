package com.mvpiq.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder
@Entity
@Table(name = "player_cv")
public class PlayerCv {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id = UUID.randomUUID();

    @OneToOne
    @JoinColumn(name = "player_id", nullable = false)
    private User player;

    @Column(name = "headline")
    @JsonProperty("headline")
    private String headline;

    @Column(name = "summary", columnDefinition = "TEXT")
    @JsonProperty("summary")
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> stats;

    @Column(name = "created_at")
    @JsonProperty("created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at")
    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @Column(name = "share_token", unique = true)
    @JsonProperty("share_token")
    private UUID shareToken;

    @Column(name = "share_enabled", nullable = false)
    @JsonProperty("share_enabled")
    private Boolean shareEnabled = false;

    @Column(name = "public_updated_at")
    @JsonProperty("public_updated_at")
    private OffsetDateTime publicUpdatedAt;

    @Column(name = "public_slug", length = 100)
    @JsonProperty("public_slug")
    private String publicSlug;
}
