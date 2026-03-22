package com.mvpiq.model;

import com.mvpiq.enums.AnalisysType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "video_analysis_types",
        uniqueConstraints = {
                @UniqueConstraint(name = "video_analysis_types_code_key", columnNames = "code")
        }
)
public class VideoAnalysisType extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    public AnalisysType code;

    @Column(nullable = false, length = 150)
    public String name;

    @Column(columnDefinition = "text")
    public String description;

    @Column(nullable = false, length = 50)
    public String sport = "basketball";

    @Column(name = "max_video_seconds", nullable = false)
    public int maxVideoSeconds = 5;

    @Column(name = "max_video_size_mb", nullable = false)
    public int maxVideoSizeMb = 10;

    @Column(name = "frames_to_extract", nullable = false)
    public int framesToExtract = 3;

    @Column(name = "ai_prompt", columnDefinition = "text")
    public String aiPrompt;

    @Column(name = "target_body_view", length = 50)
    public String targetBodyView;

    @Column(name = "reference_image_url", length = 500)
    public String referenceImageUrl;

    @Column(name = "is_active", nullable = false)
    public boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    public OffsetDateTime createdAt;

    @Column(name = "updated_at")
    public OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}