package com.mvpiq.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "video_analysis_types")
@Getter
@Setter
public class VideoAnalysisType {

    @Id
    @GeneratedValue
    public UUID id;

    public String code;

    public String name;

    public String description;

    public String sport;

    public Integer maxVideoSeconds;

    public Integer maxVideoSizeMb;

    public Integer framesToExtract;

    public String targetBodyView;

    public String referenceImageUrl;

    @Column(name = "ai_prompt")
    public String aiPrompt;

    public Boolean isActive;

    public Instant createdAt;

    public Instant updatedAt;
}
