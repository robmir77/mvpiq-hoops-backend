package com.mvpiq.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "workout_sessions")
public class WorkoutSession {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerProfile player;

    @Enumerated(EnumType.STRING)
    @Column(name = "camera_mode", nullable = false)
    private CameraMode cameraMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "court_type", nullable = false)
    private CourtType courtType;

    @Column(name = "start_time", nullable = false)
    private OffsetDateTime startTime;

    @Column(name = "end_time")
    private OffsetDateTime endTime;

    @Column(name = "total_shots")
    private Integer totalShots = 0;

    @Column(name = "made_shots")
    private Integer madeShots = 0;

    @Column(name = "session_status", nullable = false)
    private String sessionStatus = "ACTIVE"; // ACTIVE, COMPLETED, PAUSED

    @Column(name = "calibration_data", columnDefinition = "jsonb")
    private String calibrationData;

    @OneToMany(mappedBy = "workoutSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"workoutSession"})
    private List<ShotEvent> shots;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public enum CameraMode {
        LATERAL,
        FRONTAL,
        ANGLE_45
    }

    public enum CourtType {
        HALF_COURT,
        FULL_COURT
    }
}
