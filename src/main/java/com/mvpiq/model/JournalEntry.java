package com.mvpiq.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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

    @Column(name = "checklist_completed", nullable = false)
    private Boolean checklistCompleted = false;

    @Column(name = "tags", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String tags;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"journalEntry"})
    private Set<JournalChecklist> checklists = new HashSet<>();
}