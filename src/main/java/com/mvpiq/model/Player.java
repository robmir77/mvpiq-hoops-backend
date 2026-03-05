package com.mvpiq.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "players")
@PrimaryKeyJoinColumn(name = "id")
public class Player extends User {

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "height_cm")
    private Short heightCm;

    @Column(name = "weight_kg")
    private Short weightKg;

    @Column(name = "level", length = 50)
    private String level;

    @Column(name = "dominant_hand", length = 10)
    private String dominantHand;

    @Column(name = "country", length = 50)
    private String country;

    @Column(name = "city", length = 50)
    private String city;

    @Column(name = "approximate_age")
    private Integer approximateAge;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    // NUOVA RELAZIONE POSITIONS
    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlayerPosition> positions = new ArrayList<>();
}