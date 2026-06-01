package com.mvpiq.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO per EventParticipant.
 * Rappresenta la partecipazione di un singolo utente a un evento, con stato RSVP.
 */
@Data
public class EventParticipantDTO {

    private UUID id;

    /** ID dell'evento. */
    private UUID eventId;

    /** ID dell'utente partecipante. */
    private UUID userId;

    /** Display name dell'utente (read-only, popolato in output). */
    private String userDisplayName;

    /**
     * Stato RSVP: INVITED | GOING | MAYBE | NOT_GOING.
     */
    private String rsvpStatus;

    /** Timestamp risposta RSVP. */
    private OffsetDateTime respondedAt;

    /** Nota libera del partecipante. */
    private String note;

    /** Timestamp di iscrizione o invito. */
    private OffsetDateTime joinedAt;
}
