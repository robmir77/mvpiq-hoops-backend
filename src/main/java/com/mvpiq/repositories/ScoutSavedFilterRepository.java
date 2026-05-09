package com.mvpiq.repositories;

import com.mvpiq.model.ScoutSavedFilter;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ScoutSavedFilterRepository implements PanacheRepositoryBase<ScoutSavedFilter, UUID> {

    public List<ScoutSavedFilter> findByScoutId(UUID scoutId) {
        return find("scout.id", scoutId).list();
    }

    public ScoutSavedFilter findByScoutIdAndName(UUID scoutId, String name) {
        return find("scout.id = ?1 AND name = ?2", scoutId, name).firstResult();
    }

    public List<ScoutSavedFilter> findByNameContaining(UUID scoutId, String name) {
        return find("scout.id = ?1 AND LOWER(name) LIKE LOWER(?2)", scoutId, "%" + name + "%").list();
    }

    public long countByScoutId(UUID scoutId) {
        return count("scout.id", scoutId);
    }
}
