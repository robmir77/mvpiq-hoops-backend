package com.mvpiq.repositories;

import com.mvpiq.model.AthletePoints;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class AthletePointsRepository implements PanacheRepositoryBase<AthletePoints, UUID> {

    public AthletePoints findByAthlete(UUID athleteId) {
        return findById(athleteId);
    }
}