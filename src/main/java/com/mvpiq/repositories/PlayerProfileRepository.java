package com.mvpiq.repositories;

import com.mvpiq.model.PlayerProfile;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PlayerProfileRepository implements PanacheRepositoryBase<PlayerProfile, UUID> {

    public PlayerProfile findByUserId(UUID userId) {
        return find("user.id", userId).firstResult();
    }

    public List<PlayerProfile> findByCountry(String country) {
        return find("country", country).list();
    }

    public List<PlayerProfile> findByLevel(String level) {
        return find("level", level).list();
    }

    public List<PlayerProfile> findByMainPosition(String position) {
        return find("mainPosition", position).list();
    }

    public List<PlayerProfile> findByAgeRange(int minAge, int maxAge) {
        return find("approximateAge between ?1 and ?2", minAge, maxAge).list();
    }

    public List<PlayerProfile> findByPublicProfile(Boolean publicProfile) {
        return find("publicProfile", publicProfile).list();
    }
}
