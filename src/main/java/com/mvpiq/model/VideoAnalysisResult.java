package com.mvpiq.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "video_analysis_results")
public class VideoAnalysisResult {

    @Id
    @GeneratedValue
    public UUID id;

    @ManyToOne
    @JoinColumn(name = "session_id")
    public VideoAnalysisSession session;

    public Integer score;

    @Column(name = "detected_errors", columnDefinition = "jsonb")
    public String detectedErrors;

    @Column(name = "suggestions", columnDefinition = "jsonb")
    public String suggestions;

    @Column(name = "ai_response", columnDefinition = "jsonb")
    public String aiResponse;

    @Column(name = "created_at")
    public Instant createdAt;
}
