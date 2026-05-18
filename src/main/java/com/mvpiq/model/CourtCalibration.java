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
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "court_calibrations")
public class CourtCalibration {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_session_id", nullable = false)
    private WorkoutSession workoutSession;

    @Column(name = "hoop_center_x")
    private Double hoopCenterX;

    @Column(name = "hoop_center_y")
    private Double hoopCenterY;

    @Column(name = "free_throw_line_x")
    private Double freeThrowLineX;

    @Column(name = "free_throw_line_y")
    private Double freeThrowLineY;

    @Column(name = "three_point_line_top_x")
    private Double threePointLineTopX;

    @Column(name = "three_point_line_top_y")
    private Double threePointLineTopY;

    @Column(name = "three_point_line_left_x")
    private Double threePointLineLeftX;

    @Column(name = "three_point_line_left_y")
    private Double threePointLineLeftY;

    @Column(name = "three_point_line_right_x")
    private Double threePointLineRightX;

    @Column(name = "three_point_line_right_y")
    private Double threePointLineRightY;

    @Column(name = "baseline_x")
    private Double baselineX;

    @Column(name = "baseline_y")
    private Double baselineY;

    @Column(name = "sideline_left_x")
    private Double sidelineLeftX;

    @Column(name = "sideline_left_y")
    private Double sidelineLeftY;

    @Column(name = "sideline_right_x")
    private Double sidelineRightX;

    @Column(name = "sideline_right_y")
    private Double sidelineRightY;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "homography_matrix", columnDefinition = "jsonb")
    private List<Double> homographyMatrix;

    @Column(name = "calibration_confidence")
    private Double calibrationConfidence;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
