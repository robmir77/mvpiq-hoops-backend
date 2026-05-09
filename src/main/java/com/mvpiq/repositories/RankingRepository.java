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
        return find("player.id = ?1 and scope = ?2 and scopeValue = ?3 and seasonYear = ?4",
                playerId, rankScope, scopeValue, seasonYear).firstResult();
    }

    // Trova ranking per playerId e season year
    public List<Ranking> findByPlayerIdAndSeasonYearOrderByRankScope(UUID playerId, Integer seasonYear) {
        return list("player.id = ?1 and seasonYear = ?2 order by scope", playerId, seasonYear);
    }

    // Trova ranking per scope, scopeValue e season year ordinati per posizione
    public List<Ranking> findByRankScopeAndScopeValueAndSeasonYearOrderByRankPositionAsc(
            String rankScope, String scopeValue, Integer seasonYear) {
        if (scopeValue == null) {
            return list("scope = ?1 and scopeValue is null and seasonYear = ?2 order by rankPosition", 
                    rankScope, seasonYear);
        } else {
            return list("scope = ?1 and scopeValue = ?2 and seasonYear = ?3 order by rankPosition", 
                    rankScope, scopeValue, seasonYear);
        }
    }

    // Elimina ranking per scope, scopeValue e season year
    public void deleteByRankScopeAndScopeValueAndSeasonYear(String rankScope, String scopeValue, Integer seasonYear) {
        if (scopeValue == null) {
            delete("scope = ?1 and scopeValue is null and seasonYear = ?2", rankScope, seasonYear);
        } else {
            delete("scope = ?1 and scopeValue = ?2 and seasonYear = ?3", rankScope, scopeValue, seasonYear);
        }
    }
}
