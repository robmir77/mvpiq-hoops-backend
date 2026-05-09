package com.mvpiq.service;

import com.mvpiq.model.AthletePoints;
import com.mvpiq.model.TrainingProgram;
import com.mvpiq.model.TrainingSession;
import com.mvpiq.repositories.AthletePointsRepository;
import com.mvpiq.repositories.TrainingProgramRepository;
import com.mvpiq.repositories.TrainingSessionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.*;

@ApplicationScoped
public class TrainingService {

    @Inject
    TrainingProgramRepository programRepository;

    @Inject
    TrainingSessionRepository sessionRepository;

    @Inject
    AthletePointsRepository pointsRepository;

    public List<TrainingProgram> getPublicPrograms() {
        return programRepository.findPublicPrograms();
    }

    public List<TrainingSession> getUserSessions(UUID userId) {
        return sessionRepository.findByUser(userId);
    }

    public Map<String, Object> getUserStats(UUID userId) {

        long sessions = sessionRepository.countByUser(userId);

        List<TrainingSession> sessionList = sessionRepository.findByUser(userId);

        int totalSeconds = sessionList.stream()
                .map(s -> Optional.ofNullable(s.getDurationSeconds()).orElse(0))
                .reduce(0, Integer::sum);

        int minutes = totalSeconds / 60;

        Optional<AthletePoints> pointsOpt = pointsRepository.findByPlayer(userId);
        long totalPoints = pointsOpt.map(AthletePoints::getTotalPoints).orElse(0L);

        Map<String, Object> stats = new HashMap<>();
        stats.put("sessions", sessions);
        stats.put("minutes", minutes);
        stats.put("points", totalPoints);

        return stats;
    }
}