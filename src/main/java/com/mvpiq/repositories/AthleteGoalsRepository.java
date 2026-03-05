package com.mvpiq.repositories;

import com.mvpiq.model.AthleteGoal;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class AthleteGoalsRepository implements PanacheRepository<AthleteGoal> {

    // Trova tutti i goal di un atleta
    public List<AthleteGoal> findByAthleteId(UUID athleteId) {
        return list("athleteId", athleteId);
    }

    // Trova un goal per ID
    public AthleteGoal findById(UUID goalId) {
        return find("id", goalId).firstResult();
    }

    // Persist o update
    @Transactional
    public void save(AthleteGoal goal) {
        persist(goal); // Panache gestisce insert e update
    }

    // Cancellazione di un goal
    @Transactional
    public boolean deleteById(UUID goalId) {
        return delete("id", goalId) > 0;
    }
}
