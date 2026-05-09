package com.mvpiq.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "player_profiles")
@PrimaryKeyJoinColumn(name = "id")
public class PlayerProfile extends User {

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "height_cm")
    private Integer heightCm;

    @Column(name = "weight_kg")
    private Integer weightKg;

    @Column(name = "main_position", length = 10)
    private String mainPosition;

    @Column(name = "secondary_positions", columnDefinition = "TEXT")
    private String secondaryPositions;

    @Column(name = "level", length = 20)
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

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlayerPosition> positions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        setCreatedAt(OffsetDateTime.now());
        this.updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
