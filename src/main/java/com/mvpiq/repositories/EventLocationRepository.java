package com.mvpiq.repositories;

import com.mvpiq.model.EventLocation;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class EventLocationRepository implements PanacheRepositoryBase<EventLocation, UUID> {

    /** Cerca location per città (case-insensitive). */
    public List<EventLocation> findByCity(String city) {
        return find("lower(city) = lower(?1) order by name asc", city).list();
    }

    /** Cerca location per tipo di campo. */
    public List<EventLocation> findByCourtType(String courtType) {
        return find("courtType = ?1 order by name asc", courtType).list();
    }

    /** Restituisce tutte le location pubbliche. */
    public List<EventLocation> findPublic() {
        return find("isPublic = true order by name asc").list();
    }

    /** Location censite da uno specifico utente. */
    public List<EventLocation> findByCreator(UUID userId) {
        return find("createdBy.id = ?1 order by createdAt desc", userId).list();
    }
}
