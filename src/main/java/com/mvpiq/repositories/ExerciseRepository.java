package com.mvpiq.repositories;

import com.mvpiq.model.Exercise;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ExerciseRepository implements PanacheRepositoryBase<Exercise, UUID> {

    public List<Exercise> findByOwnerId(UUID ownerId) {
        return find("owner.id", ownerId).list();
    }

    public List<Exercise> findByCategory(String category) {
        return find("category", category).list();
    }

    public List<Exercise> findByDifficulty(String difficulty) {
        return find("difficulty", difficulty).list();
    }

    public List<Exercise> findByMediaType(String mediaType) {
        return find("media.mediaType", mediaType).list();
    }

    public List<Exercise> findPublicExercises() {
        return find("owner.publicProfile", true).list();
    }

    public List<Exercise> searchByTitle(String title) {
        return find("LOWER(title) LIKE LOWER(?1)", "%" + title + "%").list();
    }
}
