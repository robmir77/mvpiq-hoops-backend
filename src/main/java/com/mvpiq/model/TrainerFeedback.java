package com.mvpiq.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@Builder
@Entity
@Table(name = "trainer_feedbacks")
public class TrainerFeedback {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainer_id", nullable = false, foreignKey = @ForeignKey(name = "trainer_feedbacks_trainer_id_fkey"))
    @JsonProperty("trainer_id")
    private User trainer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false, foreignKey = @ForeignKey(name = "trainer_feedbacks_player_id_fkey"))
    @JsonProperty("player_id")
    private User player;

    @Column(name = "feedback", columnDefinition = "TEXT", nullable = false)
    @JsonProperty("feedback")
    private String feedback;

    @Column(name = "created_at", nullable = false)
    @JsonProperty("created_at")
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
    }
}
