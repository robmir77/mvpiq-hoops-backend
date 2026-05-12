package com.mvpiq.service;

import com.mvpiq.dto.ShotChartResponse;
import com.mvpiq.exception.ResourceNotFoundException;
import com.mvpiq.model.ShotEvent;
import com.mvpiq.model.WorkoutSession;
import com.mvpiq.repositories.ShotEventRepository;
import com.mvpiq.repositories.WorkoutSessionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jboss.logging.Logger;
import java.util.stream.Collectors;

@ApplicationScoped
public class ShotAnalyticsService {

    private static final Logger LOGGER = Logger.getLogger(ShotAnalyticsService.class.getName());

    @Inject
    ShotEventRepository shotEventRepository;

    @Inject
    WorkoutSessionRepository workoutSessionRepository;

    public ShotChartResponse getShotChart(UUID sessionId, UUID playerId) {
        WorkoutSession session = workoutSessionRepository.findByIdAndPlayer(sessionId, playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Workout session not found"));

        List<ShotEvent> shots = shotEventRepository.findByWorkoutSessionWithCoordinates(sessionId);
        
        List<ShotChartResponse.ShotPoint> shotPoints = shots.stream()
                .map(this::mapToShotPoint)
                .collect(Collectors.toList());

        ShotChartResponse.SessionStats sessionStats = calculateSessionStats(sessionId, shots);
        ShotChartResponse.ZoneStats zoneStats = calculateZoneStats(sessionId);

        return ShotChartResponse.builder()
                .shots(shotPoints)
                .sessionStats(sessionStats)
                .zoneStats(zoneStats)
                .build();
    }

    public ShotChartResponse.ShotPoint mapToShotPoint(ShotEvent shot) {
        String zone = determineZone(shot.getDistanceFromHoop());
        
        return ShotChartResponse.ShotPoint.builder()
                .x(shot.getCourtX())
                .y(shot.getCourtY())
                .made(ShotEvent.ShotResult.MADE.equals(shot.getShotResult()))
                .distance(shot.getDistanceFromHoop())
                .zone(zone)
                .build();
    }

    private ShotChartResponse.SessionStats calculateSessionStats(UUID sessionId, List<ShotEvent> shots) {
        long totalShots = shotEventRepository.countByWorkoutSession(sessionId);
        long madeShots = shotEventRepository.countByWorkoutSessionAndResult(sessionId, ShotEvent.ShotResult.MADE);
        long missedShots = totalShots - madeShots;
        
        Double shootingPercentage = totalShots > 0 ? (madeShots * 100.0 / totalShots) : 0.0;
        Double averageDistance = shotEventRepository.calculateAverageDistance(sessionId);

        Map<String, Long> zonePerformance = shots.stream()
                .collect(Collectors.groupingBy(
                        shot -> determineZone(shot.getDistanceFromHoop()),
                        Collectors.counting()
                ));

        String bestZone = zonePerformance.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        String worstZone = zonePerformance.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        return ShotChartResponse.SessionStats.builder()
                .totalShots((int) totalShots)
                .madeShots((int) madeShots)
                .missedShots((int) missedShots)
                .shootingPercentage(shootingPercentage)
                .averageDistance(averageDistance)
                .bestZone(bestZone)
                .worstZone(worstZone)
                .build();
    }

    private ShotChartResponse.ZoneStats calculateZoneStats(UUID sessionId) {
        List<Object[]> distributionData = shotEventRepository.getShotDistributionByZone(sessionId);
        
        ShotChartResponse.ZonePaint paint = new ShotChartResponse.ZonePaint(0, 0, 0.0);
        ShotChartResponse.ZoneMidRange midRange = new ShotChartResponse.ZoneMidRange(0, 0, 0.0);
        ShotChartResponse.ZoneThreePoint threePoint = new ShotChartResponse.ZoneThreePoint(0, 0, 0.0);
        ShotChartResponse.ZoneCorner corner = new ShotChartResponse.ZoneCorner(0, 0, 0.0);

        for (Object[] row : distributionData) {
            String zone = (String) row[0];
            Long attempts = (Long) row[1];
            Long made = (Long) row[2];
            Double percentage = attempts > 0 ? (made * 100.0 / attempts) : 0.0;

            switch (zone) {
                case "PAINT":
                    paint = new ShotChartResponse.ZonePaint(attempts.intValue(), made.intValue(), percentage);
                    break;
                case "MID_RANGE":
                    midRange = new ShotChartResponse.ZoneMidRange(attempts.intValue(), made.intValue(), percentage);
                    break;
                case "THREE_POINT":
                    threePoint = new ShotChartResponse.ZoneThreePoint(attempts.intValue(), made.intValue(), percentage);
                    break;
                case "CORNER":
                    corner = new ShotChartResponse.ZoneCorner(attempts.intValue(), made.intValue(), percentage);
                    break;
            }
        }

        return ShotChartResponse.ZoneStats.builder()
                .paint(paint)
                .midRange(midRange)
                .threePoint(threePoint)
                .corner(corner)
                .build();
    }

    private String determineZone(Double distance) {
        if (distance == null) return "UNKNOWN";
        if (distance <= 4.0) return "PAINT";
        if (distance <= 7.0) return "MID_RANGE";
        if (distance <= 8.0) return "CORNER";
        return "THREE_POINT";
    }

    public List<ShotEvent> getHotZones(UUID playerId, int limit) {
        return shotEventRepository.findRecentShotsByPlayer(playerId, limit)
                .stream()
                .filter(shot -> ShotEvent.ShotResult.MADE.equals(shot.getShotResult()))
                .collect(Collectors.toList());
    }

    public List<ShotEvent> getColdZones(UUID playerId, int limit) {
        return shotEventRepository.findRecentShotsByPlayer(playerId, limit)
                .stream()
                .filter(shot -> !ShotEvent.ShotResult.MADE.equals(shot.getShotResult()))
                .collect(Collectors.toList());
    }

    public Map<String, Double> getPlayerCareerStats(UUID playerId) {
        List<WorkoutSession> sessions = workoutSessionRepository.findCompletedSessionsByPlayer(playerId);
        
        int totalShots = sessions.stream()
                .mapToInt(session -> session.getTotalShots() != null ? session.getTotalShots() : 0)
                .sum();

        int totalMade = sessions.stream()
                .mapToInt(session -> session.getMadeShots() != null ? session.getMadeShots() : 0)
                .sum();

        double careerPercentage = totalShots > 0 ? (totalMade * 100.0 / totalShots) : 0.0;

        return Map.of(
                "totalSessions", (double) sessions.size(),
                "totalShots", (double) totalShots,
                "totalMade", (double) totalMade,
                "careerPercentage", careerPercentage
        );
    }
}
