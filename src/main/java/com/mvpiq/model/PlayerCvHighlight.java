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
@Table(name = "player_cv_highlights")
public class PlayerCvHighlight {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id = UUID.randomUUID();

    @ManyToOne
    @JoinColumn(name = "cv_id", nullable = false)
    private PlayerCv cv;

    @ManyToOne
    @JoinColumn(name = "media_id")
    private MediaAsset media;

    @Column(name = "title", length = 150)
    @JsonProperty("title")
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    @JsonProperty("description")
    private String description;

    @Column(name = "created_at")
    @JsonProperty("created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "external_url", columnDefinition = "TEXT")
    @JsonProperty("external_url")
    private String externalUrl;

    @Column(name = "sort_order", nullable = false)
    @JsonProperty("sort_order")
    private Integer sortOrder = 0;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    @JsonProperty("thumbnail_url")
    private String thumbnailUrl;
}
