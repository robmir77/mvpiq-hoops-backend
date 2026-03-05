package com.mvpiq.repositories;

import com.mvpiq.model.PlayerCvTeam;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PlayerCvTeamRepository implements PanacheRepository<PlayerCvTeam> {

    public List<PlayerCvTeam> findByPlayer(UUID playerId) {
        return list("player.id", playerId);
    }

    public void deleteByPlayer(UUID playerId) {
        delete("player.id", playerId);
    }
}