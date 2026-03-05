package com.mvpiq.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mvpiq.enums.MediaType;
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
@Table(name = "media_assets")
public class MediaAsset {

    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id = UUID.randomUUID();

    @Column(name = "owner_id", columnDefinition = "UUID")
    @JsonProperty("owner_id")
    private UUID ownerId;

    @Column(name = "title", length = 200)
    @JsonProperty("title")
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    @JsonProperty("description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, name = "media_type")
    @JsonProperty("media_type")
    private MediaType mediaType = MediaType.user_upload;

    @Column(name = "storage_url", columnDefinition = "TEXT", nullable = false)
    @JsonProperty("storage_url")
    private String storageUrl;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    @JsonProperty("thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "duration_seconds")
    @JsonProperty("duration_seconds")
    private Integer durationSeconds;

    @Column(name = "width")
    @JsonProperty("width")
    private Integer width;

    @Column(name = "height")
    @JsonProperty("height")
    private Integer height;

    @Column(name = "is_official")
    @JsonProperty("is_official")
    private Boolean isOfficial = false;

    @Column(name = "created_at")
    @JsonProperty("created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
