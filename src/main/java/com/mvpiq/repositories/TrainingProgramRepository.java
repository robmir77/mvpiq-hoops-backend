package com.mvpiq.repositories;

import com.mvpiq.model.TrainingProgram;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class TrainingProgramRepository implements PanacheRepositoryBase<TrainingProgram, UUID> {

    public List<TrainingProgram> findPublicPrograms() {
        return list("isPublic = true ORDER BY createdAt DESC");
    }

    public List<TrainingProgram> findByAuthor(UUID authorId) {
        return list("author.id = ?1 ORDER BY createdAt DESC", authorId);
    }
}