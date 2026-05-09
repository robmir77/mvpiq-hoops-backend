package com.mvpiq.service;

import com.mvpiq.model.Exercise;
import com.mvpiq.model.MediaAsset;
import com.mvpiq.model.User;
import com.mvpiq.repositories.ExerciseRepository;
import com.mvpiq.repositories.MediaAssetRepository;
import com.mvpiq.repositories.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@ApplicationScoped
public class ExerciseService {

    @Inject
    ExerciseRepository exerciseRepository;

    @Inject
    MediaAssetRepository mediaAssetRepository;

    @Inject
    UserRepository userRepository;

    @Transactional
    public Exercise createExercise(Exercise exercise, UUID ownerId, UUID mediaId) {
        log.info("Creating exercise: {} for owner: {}", exercise.getTitle(), ownerId);
        
        User owner = userRepository.findById(ownerId);
        if (owner == null) {
            throw new IllegalArgumentException("Owner not found: " + ownerId);
        }
        
        if (mediaId != null) {
            Optional<MediaAsset> media = Optional.ofNullable(mediaAssetRepository.findById(mediaId));
            media.ifPresent(exercise::setMedia);
        }
        
        exercise.setOwner(owner);
        exercise.setCreatedAt(OffsetDateTime.now());
        exercise.setUpdatedAt(OffsetDateTime.now());
        
        exerciseRepository.persist(exercise);
        log.info("Exercise created with ID: {}", exercise.getId());
        return exercise;
    }

    @Transactional
    public Exercise updateExercise(UUID exerciseId, Exercise updatedExercise) {
        log.info("Updating exercise: {}", exerciseId);
        
        Optional<Exercise> existingExercise = Optional.ofNullable(exerciseRepository.findById(exerciseId));
        if (existingExercise.isEmpty()) {
            throw new IllegalArgumentException("Exercise not found: " + exerciseId);
        }
        
        Exercise exercise = existingExercise.get();
        exercise.setTitle(updatedExercise.getTitle());
        exercise.setDescription(updatedExercise.getDescription());
        exercise.setCategory(updatedExercise.getCategory());
        exercise.setDifficulty(updatedExercise.getDifficulty());
        exercise.setDurationSeconds(updatedExercise.getDurationSeconds());
        exercise.setInstructions(updatedExercise.getInstructions());
        exercise.setUpdatedAt(OffsetDateTime.now());
        
        exerciseRepository.persist(exercise);
        log.info("Exercise updated: {}", exerciseId);
        return exercise;
    }

    @Transactional
    public void deleteExercise(UUID exerciseId) {
        log.info("Deleting exercise: {}", exerciseId);
        
        Optional<Exercise> exercise = Optional.ofNullable(exerciseRepository.findById(exerciseId));
        if (exercise.isPresent()) {
            exerciseRepository.delete(exercise.get());
            log.info("Exercise deleted: {}", exerciseId);
        }
    }

    public Optional<Exercise> getExercise(UUID exerciseId) {
        return Optional.ofNullable(exerciseRepository.findById(exerciseId));
    }

    public List<Exercise> getExercisesByOwner(UUID ownerId) {
        return exerciseRepository.findByOwnerId(ownerId);
    }

    public List<Exercise> getPublicExercises() {
        return exerciseRepository.findPublicExercises();
    }

    public List<Exercise> getExercisesByCategory(String category) {
        return exerciseRepository.findByCategory(category);
    }

    public List<Exercise> getExercisesByDifficulty(String difficulty) {
        return exerciseRepository.findByDifficulty(difficulty);
    }

    public List<Exercise> searchExercises(String title) {
        return exerciseRepository.searchByTitle(title);
    }

    public List<Exercise> getExercisesByMediaType(String mediaType) {
        return exerciseRepository.findByMediaType(mediaType);
    }
}
