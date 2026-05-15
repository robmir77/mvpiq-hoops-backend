package com.mvpiq.repositories;

import com.mvpiq.model.PlayerCvHighlight;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PlayerCvHighlightRepository implements PanacheRepository<PlayerCvHighlight> {

    public List<PlayerCvHighlight> findByCvId(UUID cvId) {
        return find("cv.id", cvId).list();
    }

    public List<PlayerCvHighlight> findByCvIdOrderBySortOrder(UUID cvId) {
        return find("cv.id = ?1 ORDER BY sortOrder ASC", cvId).list();
    }

    public List<PlayerCvHighlight> findByMediaId(UUID mediaId) {
        return find("media.id", mediaId).list();
    }

    public List<PlayerCvHighlight> findByTitleContaining(String title) {
        return find("LOWER(title) LIKE LOWER(?1)", "%" + title + "%").list();
    }

    public long countByCvId(UUID cvId) {
        return count("cv.id", cvId);
    }

    public Integer findMaxSortOrderByCvId(UUID cvId) {
        return getEntityManager()
                .createQuery("SELECT MAX(h.sortOrder) FROM PlayerCvHighlight h WHERE h.cv.id = :cvId", Integer.class)
                .setParameter("cvId", cvId)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    public Optional<PlayerCvHighlight> findByIdOptional(UUID id) {
        return find("id", id).firstResultOptional();
    }
}
