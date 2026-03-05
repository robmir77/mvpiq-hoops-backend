package com.mvpiq.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder
@Entity
@Table(name = "player_cv_team_categories")
public class PlayerCvTeamCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "code", length = 50, nullable = false, unique = true)
    @JsonProperty("code")
    private String code;

    @Column(name = "label", length = 100, nullable = false)
    @JsonProperty("label")
    private String label;
}
