package com.mvpiq.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "pose_analysis")
public class PoseAnalysis {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shot_event_id")
    private ShotEvent shotEvent;

    @Column(name = "elbow_angle", precision = 5, scale = 2)
    private BigDecimal elbowAngle;

    @Column(name = "knee_angle", precision = 5, scale = 2)
    private BigDecimal kneeAngle;

    @Column(name = "shoulder_angle", precision = 5, scale = 2)
    private BigDecimal shoulderAngle;

    @Column(name = "wrist_angle", precision = 5, scale = 2)
    private BigDecimal wristAngle;

    @Column(name = "release_height", precision = 10, scale = 2)
    private BigDecimal releaseHeight;

    @Column(name = "release_angle", precision = 5, scale = 2)
    private BigDecimal releaseAngle;

    @Column(name = "release_velocity", precision = 10, scale = 2)
    private BigDecimal releaseVelocity;

    @Column(name = "shot_smoothness", precision = 5, scale = 2)
    private BigDecimal shotSmoothness;

    @Column(name = "follow_through_score", precision = 5, scale = 2)
    private BigDecimal followThroughScore;

    @Column(name = "balance_score", precision = 5, scale = 2)
    private BigDecimal balanceScore;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
