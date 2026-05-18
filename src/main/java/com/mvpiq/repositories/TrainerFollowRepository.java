package com.mvpiq.repositories;

import com.mvpiq.model.TrainerFollows;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class TrainerFollowRepository implements PanacheRepositoryBase<TrainerFollows, UUID> {

    public List<TrainerFollows> findByTrainerId(UUID trainerId) {
        return find("trainer.id", trainerId).list();
    }

    public List<TrainerFollows> findByPlayerId(UUID playerId) {
        return find("player.id", playerId).list();
    }

    public TrainerFollows findByTrainerAndPlayer(UUID trainerId, UUID playerId) {
        return find("trainer.id = ?1 AND player.id = ?2", trainerId, playerId).firstResult();
    }

    public boolean existsByTrainerAndPlayer(UUID trainerId, UUID playerId) {
        return count("trainer.id = ?1 AND player.id = ?2", trainerId, playerId) > 0;
    }

    public long countByTrainerId(UUID trainerId) {
        return count("trainer.id", trainerId);
    }

    public long countByPlayerId(UUID playerId) {
        return count("player.id", playerId);
    }
}
