package com.mvpiq.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO per Event.
 * Usato sia in input (creazione/aggiornamento) sia in output (risposta API).
 * In output i partecipanti vengono inclusi quando richiesto esplicitamente.
 */
@Data
public class EventDTO {

    private UUID id;

    /** ID dell'utente organizzatore. In output viene incluso automaticamente. */
    private UUID creatorId;

    /** Display name del creatore (read-only, popolato in output). */
    private String creatorDisplayName;

    /**
     * Tipo evento: PICKUP | TRAINING | TOURNAMENT | SOCIAL.
     * Obbligatorio in creazione.
     */
    private String type;

    /** Titolo breve e descrittivo. Obbligatorio in creazione. */
    private String title;

    /** Descrizione estesa: regole, livello, cosa portare, ecc. */
    private String description;

    /** Data e ora di inizio. Obbligatorio in creazione. */
    private OffsetDateTime startsAt;

    /** Data e ora di fine. Opzionale. */
    private OffsetDateTime endsAt;

    /** Numero massimo di partecipanti. NULL = nessun limite. */
    private Integer maxParticipants;

    /**
     * Stato: DRAFT | OPEN | FULL | CANCELLED | COMPLETED.
     * Default: OPEN.
     */
    private String status;

    /**
     * Visibilità: PUBLIC | FRIENDS | PRIVATE.
     * Default: PUBLIC.
     */
    private String visibility;

    /** Luogo dell'evento. In input basta locationId; in output viene restituito l'oggetto completo. */
    private UUID locationId;
    private EventLocationDTO location;

    /** Tag liberi (es. ["3x3","competitivo","under20"]). */
    private String tags;

    /** Numero di partecipanti con RSVP = GOING (read-only, popolato in output). */
    private Integer participantCount;

    /** Lista partecipanti (opzionale, inclusa solo nelle chiamate di dettaglio). */
    private List<EventParticipantDTO> participants;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
