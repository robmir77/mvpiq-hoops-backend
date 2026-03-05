package com.mvpiq.repositories;

import com.mvpiq.model.PlayerCv;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PlayerCvRepository implements PanacheRepository<PlayerCv> {

    public Optional<PlayerCv> findByPlayer(UUID playerId) {
        return find("player.id", playerId).firstResultOptional();
    }
}