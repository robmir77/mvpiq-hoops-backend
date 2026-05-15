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

    public Optional<PlayerCv> findByPlayerId(UUID playerId) {
        return find("player.id", playerId).firstResultOptional();
    }

    public Optional<PlayerCv> findByPlayerIdDirect(UUID playerId) {
        return find("player.id", playerId).firstResultOptional();
    }

    public Optional<PlayerCv> findByPlayerIdNative(UUID playerId) {
        return find("player.id", playerId).firstResultOptional();
    }

    public Optional<PlayerCv> findByPlayerIdColumn(UUID playerId) {
        return find("player.id", playerId).firstResultOptional();
    }

    public Optional<PlayerCv> findByPlayerIdNativeQuery(UUID playerId) {
        return find("SELECT cv FROM PlayerCv cv WHERE cv.player.id = ?1", playerId).firstResultOptional();
    }

    public Optional<PlayerCv> findByShareToken(UUID shareToken) {
        return find("shareToken", shareToken).firstResultOptional();
    }

    public Optional<PlayerCv> findByShareTokenAndEnabled(UUID shareToken) {
        return find("shareToken = ?1 and shareEnabled = true", shareToken).firstResultOptional();
    }
}