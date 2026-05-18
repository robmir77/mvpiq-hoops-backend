package com.mvpiq.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "workout_frame_data")
public class WorkoutFrameData {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private WorkoutSession session;

    @Column(name = "frame_timestamp", nullable = false)
    private Long frameTimestamp;

    @Column(name = "ball_x")
    private Double ballX;

    @Column(name = "ball_y")
    private Double ballY;

    @Column(name = "ball_confidence")
    private Double ballConfidence;

    @Column(name = "hoop_x")
    private Double hoopX;

    @Column(name = "hoop_y")
    private Double hoopY;

    @Column(name = "hoop_confidence")
    private Double hoopConfidence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pose_data", columnDefinition = "jsonb")
    private Map<String, Object> poseData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "trajectory_data", columnDefinition = "jsonb")
    private Map<String, Object> trajectoryData;

    @Column(name = "ball_velocity_x")
    private Double ballVelocityX;

    @Column(name = "ball_velocity_y")
    private Double ballVelocityY;

    @Column(name = "shot_detected")
    private Boolean shotDetected = false;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
