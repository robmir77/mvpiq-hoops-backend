package com.mvpiq.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "video_analysis_frames")
public class VideoAnalysisFrame extends PanacheEntityBase {

    @Id
    public UUID id;

    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    public VideoAnalysisSession session;

    @Column(name = "frame_index")
    public int frameIndex;

    @Column(name = "frame_url")
    public String frameUrl;

    @Column(name = "created_at")
    public OffsetDateTime createdAt;
}
