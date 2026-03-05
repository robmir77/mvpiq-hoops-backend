package com.mvpiq.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "player_profile_positions",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"profile_id", "position_id"})
        }
)
public class PlayerPosition {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "profile_id", nullable = false)
    private Player player;

    @ManyToOne
    @JoinColumn(name = "position_id", nullable = false)
    private PositionMetadata position;

    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false;

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();
}