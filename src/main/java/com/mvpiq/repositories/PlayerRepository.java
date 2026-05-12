package com.mvpiq.repositories;

import com.mvpiq.model.Player;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PlayerRepository implements PanacheRepositoryBase<Player, UUID> {

    public Optional<Player> findByUserId(UUID userId) {
        return findByIdOptional(userId);
    }

    public List<Player> findByCountry(String country) {
        return list("country", country);
    }

    public List<Player> findByLevel(String level) {
        return list("level", level);
    }

    public List<Player> findByAgeRange(int minAge, int maxAge) {
        return list("approximateAge between ?1 and ?2", minAge, maxAge);
    }
}