package com.mvpiq.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "journal_entries")
public class JournalEntry {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(name = "entry_type", length = 20, nullable = false)
    private String entryType; // MATCH | TRAINING

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "entry_date", nullable = false)
    private OffsetDateTime entryDate;

    @Column(name = "opponent", length = 200)
    private String opponent;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "mood_rating")
    private Short moodRating;

    @Column(name = "performance_rating")
    private Short performanceRating;

    @Column(name = "visibility", length = 20, nullable = false)
    private String visibility; // PRIVATE | TRAINER | PUBLIC

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JournalChecklist> checklists = new ArrayList<>();
}