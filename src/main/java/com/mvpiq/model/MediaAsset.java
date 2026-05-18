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

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "visibility", length = 20)
    private String visibility = "PRIVATE";

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "storage_provider", length = 30)
    private String storageProvider = "SUPABASE";

    @Column(name = "storage_bucket", length = 100)
    private String storageBucket;

    @Column(name = "storage_path", columnDefinition = "TEXT")
    private String storagePath;

    // Aggiunto con migration CV: URL esterno (YouTube, Vimeo, ecc.)
    @Column(name = "external_url", columnDefinition = "TEXT")
    private String externalUrl;
}