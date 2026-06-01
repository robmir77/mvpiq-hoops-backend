package com.mvpiq.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Evento organizzato da un utente.
 * Tipi supportati: PICKUP (partita al campetto), TRAINING (allenamento di gruppo),
 * TOURNAMENT (torneo), SOCIAL (ritrovo generico).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue
    private UUID id;

    /** Utente organizzatore dell'evento. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    /** Luogo dell'evento. NULL se ancora da definire al momento della creazione. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private EventLocation location;

    /**
     * Tipo evento: PICKUP | TRAINING | TOURNAMENT | SOCIAL.
     */
    @Column(name = "type", length = 30, nullable = false)
    private String type;

    /** Titolo breve e descrittivo dell'evento. */
    @Column(name = "title", length = 200, nullable = false)
    private String title;

    /** Descrizione estesa: regole, livello richiesto, cosa portare, ecc. */
    @Column(name = "description", columnDefinition = "text")
    private String description;

    /** Data e ora di inizio dell'evento. */
    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    /** Data e ora di fine prevista. NULL se non definita. Deve essere > startsAt. */
    @Column(name = "ends_at")
    private OffsetDateTime endsAt;

    /**
     * Numero massimo di partecipanti. NULL = nessun limite.
     * Quando raggiunto, lo status viene impostato a FULL dal service.
     */
    @Column(name = "max_participants")
    private Integer maxParticipants;

    /**
     * Stato dell'evento: DRAFT | OPEN | FULL | CANCELLED | COMPLETED.
     */
    @Column(name = "status", length = 20, nullable = false)
    private String status = "OPEN";

    /**
     * Visibilità: PUBLIC (tutti) | FRIENDS (seguiti dal creatore) | PRIVATE (solo invitati).
     */
    @Column(name = "visibility", length = 20, nullable = false)
    private String visibility = "PUBLIC";

    /** Array JSON di tag liberi per filtrare gli eventi (es. ["3x3","competitivo"]). */
    @Column(name = "tags", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String tags;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    /**
     * Soft delete: se valorizzato l'evento è eliminato e non appare nelle query standard.
     */
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    /** Partecipazioni all'evento. */
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"event"})
    @Builder.Default
    private Set<EventParticipant> participants = new HashSet<>();
}
