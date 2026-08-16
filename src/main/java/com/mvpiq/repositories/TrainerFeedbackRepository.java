package com.mvpiq.repositories;

import com.mvpiq.model.TrainerFeedback;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class TrainerFeedbackRepository implements PanacheRepositoryBase<TrainerFeedback, UUID> {

    public List<TrainerFeedback> findByPlayerId(UUID playerId) {
        return find("player.id = ?1 order by createdAt desc", playerId).list();
    }

    public List<TrainerFeedback> findByTrainerId(UUID trainerId) {
        return find("trainer.id = ?1 order by createdAt desc", trainerId).list();
    }
}
