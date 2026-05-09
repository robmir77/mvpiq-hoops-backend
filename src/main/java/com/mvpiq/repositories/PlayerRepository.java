package com.mvpiq.repositories;

import com.mvpiq.model.PlayerProfile;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PlayerRepository implements PanacheRepositoryBase<PlayerProfile, UUID> {

    public Optional<PlayerProfile> findByUsername(String username) {
        return find("username", username).firstResultOptional();
    }

    public List<PlayerProfile> findByCountry(String country) {
        return list("country", country);
    }

    public List<PlayerProfile> findVerifiedPlayers() {
        return list("verified", true);
    }

    public Optional<PlayerProfile> findByUserId(UUID id) {
        // semanticamente ora è semplicemente id
        return findByIdOptional(id);
    }
}