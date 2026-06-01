package com.mvpiq.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Partecipazione di un utente a un evento, con stato RSVP.
 * Un record nasce da auto-iscrizione (GOING) o da invito esplicito del creatore (INVITED).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "event_participants",
    uniqueConstraints = @UniqueConstraint(
        name = "event_participants_event_user_uk",
        columnNames = {"event_id", "user_id"}
    )
)
public class EventParticipant {

    @Id
    @GeneratedValue
    private UUID id;

    /** Evento a cui si riferisce la partecipazione. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    /** Utente partecipante o invitato. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Stato RSVP: INVITED | GOING | MAYBE | NOT_GOING.
     */
    @Column(name = "rsvp_status", length = 20, nullable = false)
    private String rsvpStatus = "GOING";

    /** Timestamp in cui l'utente ha aggiornato il proprio RSVP. */
    @Column(name = "responded_at")
    private OffsetDateTime respondedAt;

    /** Nota libera del partecipante (es. "porto il pallone", "arrivo tardi"). */
    @Column(name = "note", columnDefinition = "text")
    private String note;

    /** Timestamp di creazione del record (auto-iscrizione o invito). */
    @Column(name = "joined_at", nullable = false)
    private OffsetDateTime joinedAt = OffsetDateTime.now();
}
