package com.mvpiq.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "shot_events")
public class ShotEvent {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_session_id", nullable = false)
    private WorkoutSession workoutSession;

    @Column(name = "timestamp_ms", nullable = false)
    private Long timestampMs;

    @Enumerated(EnumType.STRING)
    @Column(name = "shot_result", nullable = false)
    private ShotResult shotResult;

    @Column(name = "court_x")
    private Double courtX;

    @Column(name = "court_y")
    private Double courtY;

    @Column(name = "distance_from_hoop")
    private Double distanceFromHoop;

    @Column(name = "release_angle")
    private Double releaseAngle;

    @Column(name = "release_velocity")
    private Double releaseVelocity;

    @Column(name = "shot_arc_height")
    private Double shotArcHeight;

    @Column(name = "video_timestamp_ms")
    private Long videoTimestampMs;

    @Column(name = "detection_confidence")
    private Double detectionConfidence;

    @Column(name = "tracking_data", columnDefinition = "jsonb")
    private String trackingData;

    @Column(name = "video_clip_path")
    private String videoClipPath;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public enum ShotResult {
        MADE,
        MISS,
        BLOCKED,
        AIRBALL
    }
}
