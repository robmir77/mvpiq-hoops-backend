package com.mvpiq.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "media_assets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaAsset {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "owner_id")
    private UUID ownerId;

    private String title;

    private String description;

    @Column(name = "media_type", nullable = false)
    private String mediaType;

    @Column(name = "storage_url", nullable = false)
    private String storageUrl;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    private Integer width;

    private Integer height;

    @Column(name = "is_official")
    private Boolean isOfficial;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}