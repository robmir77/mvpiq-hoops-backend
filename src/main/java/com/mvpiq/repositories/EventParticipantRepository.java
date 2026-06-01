package com.mvpiq.repositories;

import com.mvpiq.model.EventParticipant;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class EventParticipantRepository implements PanacheRepositoryBase<EventParticipant, UUID> {

    /** Tutti i partecipanti di un evento. */
    public List<EventParticipant> findByEvent(UUID eventId) {
        return find("event.id = ?1 order by joinedAt asc", eventId).list();
    }

    /** Partecipanti di un evento filtrati per stato RSVP. */
    public List<EventParticipant> findByEventAndStatus(UUID eventId, String rsvpStatus) {
        return find("event.id = ?1 and rsvpStatus = ?2 order by joinedAt asc", eventId, rsvpStatus).list();
    }

    /** Trova la partecipazione specifica di un utente a un evento. */
    public Optional<EventParticipant> findByEventAndUser(UUID eventId, UUID userId) {
        return find("event.id = ?1 and user.id = ?2", eventId, userId).firstResultOptional();
    }

    /** Verifica se un utente è già registrato a un evento. */
    public boolean existsByEventAndUser(UUID eventId, UUID userId) {
        return count("event.id = ?1 and user.id = ?2", eventId, userId) > 0;
    }

    /** Conta i partecipanti GOING di un evento. */
    public long countGoingByEvent(UUID eventId) {
        return count("event.id = ?1 and rsvpStatus = 'GOING'", eventId);
    }

    /** Tutti gli eventi futuri a cui un utente ha confermato la presenza. */
    public List<EventParticipant> findGoingByUser(UUID userId) {
        return find("""
            user.id = ?1 and rsvpStatus = 'GOING'
            order by event.startsAt asc
            """, userId).list();
    }
}
