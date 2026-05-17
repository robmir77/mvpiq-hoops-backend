package com.mvpiq.service;

import com.mvpiq.dto.CareerStatsDTO;
import com.mvpiq.dto.ShotChartResponse;
import com.mvpiq.exception.ResourceNotFoundException;
import com.mvpiq.model.ShotEvent;
import com.mvpiq.model.WorkoutSession;
import com.mvpiq.repositories.ShotEventRepository;
import com.mvpiq.repositories.WorkoutSessionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class ShotAnalyticsService {

    private static final Logger LOGGER = Logger.getLogger(ShotAnalyticsService.class.getName());

    @Inject ShotEventRepository shotEventRepository;
    @Inject WorkoutSessionRepository workoutSessionRepository;

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
        long totalShots  = shotEventRepository.countByWorkoutSession(sessionId);
        long madeShots   = shotEventRepository.countByWorkoutSessionAndResult(sessionId, ShotEvent.ShotResult.MADE);
        long missedShots = totalShots - madeShots;
        Double shootingPct   = totalShots > 0 ? (madeShots * 100.0 / totalShots) : 0.0;
        Double averageDistance = shotEventRepository.calculateAverageDistance(sessionId);

        Map<String, Long> zonePerformance = shots.stream()
                .collect(Collectors.groupingBy(
                        s -> determineZone(s.getDistanceFromHoop()), Collectors.counting()));

        String bestZone  = zonePerformance.entrySet().stream().max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("N/A");
        String worstZone = zonePerformance.entrySet().stream().min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("N/A");

        return ShotChartResponse.SessionStats.builder()
                .totalShots((int) totalShots)
                .madeShots((int) madeShots)
                .missedShots((int) missedShots)
                .shootingPercentage(shootingPct)
                .averageDistance(averageDistance)
                .bestZone(bestZone)
                .worstZone(worstZone)
                .build();
    }

    private ShotChartResponse.ZoneStats calculateZoneStats(UUID sessionId) {
        List<Object[]> distributionData = shotEventRepository.getShotDistributionByZone(sessionId);

        ShotChartResponse.ZonePaint      paint      = new ShotChartResponse.ZonePaint(0, 0, 0.0);
        ShotChartResponse.ZoneMidRange   midRange   = new ShotChartResponse.ZoneMidRange(0, 0, 0.0);
        ShotChartResponse.ZoneThreePoint threePoint = new ShotChartResponse.ZoneThreePoint(0, 0, 0.0);
        ShotChartResponse.ZoneCorner     corner     = new ShotChartResponse.ZoneCorner(0, 0, 0.0);

        for (Object[] row : distributionData) {
            String zone     = (String) row[0];
            Long attempts   = (Long) row[1];
            Long made       = (Long) row[2];
            Double pct      = attempts > 0 ? (made * 100.0 / attempts) : 0.0;
            switch (zone) {
                case "PAINT"       -> paint      = new ShotChartResponse.ZonePaint(attempts.intValue(), made.intValue(), pct);
                case "MID_RANGE"   -> midRange   = new ShotChartResponse.ZoneMidRange(attempts.intValue(), made.intValue(), pct);
                case "THREE_POINT" -> threePoint = new ShotChartResponse.ZoneThreePoint(attempts.intValue(), made.intValue(), pct);
                case "CORNER"      -> corner     = new ShotChartResponse.ZoneCorner(attempts.intValue(), made.intValue(), pct);
            }
        }

        return ShotChartResponse.ZoneStats.builder()
                .paint(paint).midRange(midRange).threePoint(threePoint).corner(corner)
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
        return shotEventRepository.findRecentShotsByPlayer(playerId, limit).stream()
                .filter(s -> ShotEvent.ShotResult.MADE.equals(s.getShotResult()))
                .collect(Collectors.toList());
    }

    public List<ShotEvent> getColdZones(UUID playerId, int limit) {
        return shotEventRepository.findRecentShotsByPlayer(playerId, limit).stream()
                .filter(s -> !ShotEvent.ShotResult.MADE.equals(s.getShotResult()))
                .collect(Collectors.toList());
    }

    // ─── Fix: restituisce CareerStatsDTO tipizzato invece di Map<String,Double> ──
    // Il FE si aspetta campi nominati (totalSessions, totalMade, overallPercentage,
    // favoriteZone) — un Map non serializza con i nomi giusti e non include favoriteZone.
    public CareerStatsDTO getPlayerCareerStats(UUID playerId) {
        List<WorkoutSession> sessions = workoutSessionRepository.findCompletedSessionsByPlayer(playerId);

        int totalShots  = sessions.stream().mapToInt(s -> s.getTotalShots() != null ? s.getTotalShots() : 0).sum();
        int totalMade   = sessions.stream().mapToInt(s -> s.getMadeShots()  != null ? s.getMadeShots()  : 0).sum();
        int totalMissed = totalShots - totalMade;
        double overallPct = totalShots > 0 ? (totalMade * 100.0 / totalShots) : 0.0;

        // Best session percentage
        double bestSessionPct = sessions.stream()
                .filter(s -> s.getTotalShots() != null && s.getTotalShots() > 0)
                .mapToDouble(s -> s.getMadeShots() * 100.0 / s.getTotalShots())
                .max().orElse(0.0);

        // Favorite zone: zona con più tiri effettuati dall'atleta in tutte le sessioni
        String favoriteZone = sessions.stream()
                .map(WorkoutSession::getId)
                .flatMap(sid -> shotEventRepository.findByWorkoutSessionWithCoordinates(sid).stream())
                .collect(Collectors.groupingBy(
                        s -> determineZone(s.getDistanceFromHoop()), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        return CareerStatsDTO.builder()
                .totalSessions(sessions.size())
                .totalShots(totalShots)
                .totalMade(totalMade)
                .totalMissed(totalMissed)
                .overallPercentage(overallPct)
                .bestSessionPercentage(bestSessionPct)
                .favoriteZone(favoriteZone)
                .build();
    }
}
