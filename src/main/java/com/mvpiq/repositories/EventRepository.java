package com.mvpiq.repositories;

import com.mvpiq.model.Event;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class EventRepository implements PanacheRepositoryBase<Event, UUID> {

    /** Tutti gli eventi attivi (non eliminati, non cancellati) ordinati per data inizio. */
    public List<Event> findActive() {
        return find("""
            deletedAt is null and status != 'CANCELLED'
            order by startsAt asc
            """).list();
    }

    /** Eventi attivi visibili pubblicamente. */
    public List<Event> findPublic() {
        return find("""
            deletedAt is null and visibility = 'PUBLIC'
            and status not in ('CANCELLED', 'DRAFT')
            order by startsAt asc
            """).list();
    }

    /** Eventi creati da un utente specifico (inclusi draft e cancellati). */
    public List<Event> findByCreator(UUID creatorId) {
        return find("""
            creator.id = ?1 and deletedAt is null
            order by startsAt desc
            """, creatorId).list();
    }

    /** Trova un evento per ID escludendo i soft-deleted. */
    public Optional<Event> findActiveById(UUID id) {
        return find("id = ?1 and deletedAt is null", id).firstResultOptional();
    }

    /** Trova un evento per ID con fetch eager dei partecipanti e della location. */
    public Optional<Event> findByIdWithParticipants(UUID id) {
        return getEntityManager()
                .createQuery("""
                    select distinct e from Event e
                    left join fetch e.participants p
                    left join fetch p.user
                    left join fetch e.location
                    where e.id = :id and e.deletedAt is null
                    """, Event.class)
                .setParameter("id", id)
                .getResultStream()
                .findFirst();
    }

    /** Prossimi eventi (futuri), filtrabili per tipo. */
    public List<Event> findUpcoming(String type) {
        if (type != null && !type.isBlank()) {
            return find("""
                deletedAt is null
                and status not in ('CANCELLED', 'DRAFT')
                and startsAt > ?1
                and type = ?2
                order by startsAt asc
                """, OffsetDateTime.now(), type).list();
        }
        return find("""
            deletedAt is null
            and status not in ('CANCELLED', 'DRAFT')
            and startsAt > ?1
            order by startsAt asc
            """, OffsetDateTime.now()).list();
    }

    /** Eventi a cui un utente partecipa (qualsiasi stato RSVP tranne NOT_GOING). */
    public List<Event> findByParticipant(UUID userId) {
        return getEntityManager()
                .createQuery("""
                    select distinct e from Event e
                    join e.participants p
                    where p.user.id = :userId
                    and p.rsvpStatus != 'NOT_GOING'
                    and e.deletedAt is null
                    order by e.startsAt asc
                    """, Event.class)
                .setParameter("userId", userId)
                .getResultList();
    }
}
