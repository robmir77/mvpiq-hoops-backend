package com.mvpiq.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "video_analysis_sessions")
@Getter
@Setter
public class VideoAnalysisSession {

    @Id
    @GeneratedValue
    public UUID id;

    public UUID userId;

    @ManyToOne
    @JoinColumn(name = "analysis_type_id")
    public VideoAnalysisType analysisType;

    public String videoUrl;

    public Integer videoSizeMb;

    public Integer videoSeconds;

    public String status;

    public Instant createdAt;

    public Instant processedAt;
}