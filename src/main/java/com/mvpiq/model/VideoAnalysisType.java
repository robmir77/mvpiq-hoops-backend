package com.mvpiq.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "video_analysis_types")
public class VideoAnalysisType extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(nullable = false, length = 60)
    public String code;

    @Column(nullable = false, length = 150)
    public String name;

    public String description;

    @Column(nullable = false)
    public String sport;

    @Column(name = "max_video_seconds")
    public int maxVideoSeconds;

    @Column(name = "max_video_size_mb")
    public int maxVideoSizeMb;

    @Column(name = "frames_to_extract")
    public int framesToExtract;

    @Column(name = "ai_prompt")
    public String aiPrompt;

    @Column(name = "target_body_view")
    public String targetBodyView;

    @Column(name = "reference_image_url")
    public String referenceImageUrl;

    @Column(name = "is_active")
    public boolean isActive;

    @Column(name = "created_at")
    public OffsetDateTime createdAt;

    @Column(name = "updated_at")
    public OffsetDateTime updatedAt;
}
