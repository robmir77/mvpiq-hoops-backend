package com.mvpiq.model;

import jakarta.persistence.*;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "video_analysis_sessions")
public class VideoAnalysisSession extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(name = "user_id", nullable = false)
    public UUID userId;

    @ManyToOne
    @JoinColumn(name = "analysis_type_id", nullable = false)
    public VideoAnalysisType analysisType;

    @Column(name = "video_url", nullable = false)
    public String videoUrl;

    @Column(name = "video_size_mb")
    public Integer videoSizeMb;

    @Column(name = "video_seconds")
    public Integer videoSeconds;

    public String status;

    @Column(name = "created_at")
    public OffsetDateTime createdAt;

    @Column(name = "processed_at")
    public OffsetDateTime processedAt;
}