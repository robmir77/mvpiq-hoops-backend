package com.mvpiq.repositories;

import com.mvpiq.model.AthleteBadge;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class AthleteBadgeRepository implements PanacheRepositoryBase<AthleteBadge, UUID> {

    public List<AthleteBadge> findByPlayerId(UUID playerId) {
        return list("from AthleteBadge where player.id = ?1", playerId);
    }

    public List<AthleteBadge> findByPlayerIdOrderByObtainedAtDesc(UUID playerId) {
        return list("from AthleteBadge where player.id = ?1 order by obtainedAt desc", playerId);
    }

    public boolean existsByPlayerIdAndBadgeId(UUID playerId, UUID badgeId) {
        return count("player.id = ?1 and badgeId = ?2", playerId, badgeId) > 0;
    }

    public long countByPlayerId(UUID playerId) {
        return count("from AthleteBadge where player.id = ?1", playerId);
    }

    public List<AthleteBadge> findByBadgeId(UUID badgeId) {
        return list("badgeId", badgeId);
    }

    public List<AthleteBadge> findRecentBadges(UUID playerId, int limit) {
        return find("from AthleteBadge where player.id = ?1 order by obtainedAt desc", playerId).page(0, limit).list();
    }
}
