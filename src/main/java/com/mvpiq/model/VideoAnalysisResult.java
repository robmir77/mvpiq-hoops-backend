package com.mvpiq.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "video_analysis_results")
public class VideoAnalysisResult extends PanacheEntityBase {

    @Id
    public UUID id;

    @OneToOne
    @JoinColumn(name = "session_id", nullable = false)
    public VideoAnalysisSession session;

    public Integer score;

    @Column(columnDefinition = "jsonb")
    public String detectedErrors;

    @Column(columnDefinition = "jsonb")
    public String suggestions;

    @Column(name = "ai_response", columnDefinition = "jsonb")
    public String aiResponse;

    @Column(name = "created_at")
    public OffsetDateTime createdAt;
}
