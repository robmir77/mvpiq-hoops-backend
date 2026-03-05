package com.mvpiq.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "player_cv_teams")
@Getter
@Setter
public class PlayerCvTeam {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id = UUID.randomUUID();

    @ManyToOne
    @JoinColumn(name = "cv_id", nullable = false)
    private PlayerCv cv;

    @Column(name = "team_name", nullable = false)
    private String teamName;

    @Column(name = "category_id", nullable = false)
    private Integer categoryId;

    @ManyToOne
    @JoinColumn(name = "position_id")
    private PositionMetadata position;

    private Integer startYear;
    private Integer endYear;

    @Column(columnDefinition = "TEXT")
    private String notes;
}