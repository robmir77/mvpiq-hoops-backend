package com.mvpiq.repositories;

import com.mvpiq.model.Player;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PlayerRepository implements PanacheRepositoryBase<Player, UUID> {

    public Optional<Player> findByUsername(String username) {
        return find("username", username).firstResultOptional();
    }

    public List<Player> findByCountry(String country) {
        return list("country", country);
    }

    public List<Player> findVerifiedPlayers() {
        return list("verified", true);
    }

    public Optional<Player> findByUserId(UUID id) {
        // semanticamente ora è semplicemente id
        return findByIdOptional(id);
    }
}