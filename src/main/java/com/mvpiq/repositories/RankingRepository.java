package com.mvpiq.repositories;

import com.mvpiq.enums.UserRole;
import com.mvpiq.model.Ranking;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class RankingRepository implements PanacheRepositoryBase<Ranking, UUID> {

    // Trova tutti i ranking per un ruolo specifico
    public List<Ranking> findByRole(UserRole role) {
        return list("player.role = ?1", role);
    }

    // Trova tutti i ranking globali
    public List<Ranking> findGlobalRanking() {
        return list("rankScope = ?1", "GLOBAL");
    }

    public List<Ranking> findByRoleCode(String roleCode) {
        return list("rankScope = :scope AND scopeValue = :value ORDER BY score DESC",
                Parameters.with("scope", "ROLE").and("value", roleCode));
    }

    // Trova ranking per playerId e scope
    public Ranking findByPlayerAndScope(UUID playerId, String rankScope, String scopeValue, Integer seasonYear) {
        return find("player_id = ?1 and rank_scope = ?2 and scope_value = ?3 and season_year = ?4",
                playerId, rankScope, scopeValue, seasonYear).firstResult();
    }
}
