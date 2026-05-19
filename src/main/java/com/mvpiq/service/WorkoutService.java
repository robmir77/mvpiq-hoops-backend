package com.mvpiq.service;

import com.mvpiq.dto.CalibrationRequest;
import com.mvpiq.dto.FrameDataRequest;
import com.mvpiq.dto.PoseAnalysisRequest;
import com.mvpiq.dto.RealtimeStatsResponse;
import com.mvpiq.dto.ShotEventRequest;
import com.mvpiq.dto.WorkoutSessionRequest;
import com.mvpiq.dto.WorkoutSessionResponse;
import com.mvpiq.exception.ResourceNotFoundException;
import com.mvpiq.model.CourtCalibration;
import com.mvpiq.model.PoseAnalysis;
import com.mvpiq.model.Player;
import com.mvpiq.model.ShotEvent;
import com.mvpiq.model.WorkoutFrameData;
import com.mvpiq.model.WorkoutSession;
import com.mvpiq.repositories.CourtCalibrationRepository;
import com.mvpiq.repositories.PlayerRepository;
import com.mvpiq.repositories.PoseAnalysisRepository;
import com.mvpiq.repositories.ShotEventRepository;
import com.mvpiq.repositories.WorkoutFrameDataRepository;
import com.mvpiq.repositories.WorkoutSessionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

import static io.quarkus.arc.impl.UncaughtExceptions.LOGGER;

@ApplicationScoped
public class WorkoutService {

    private static final Logger LOGGER = Logger.getLogger(WorkoutService.class.getName());

    @Inject
    WorkoutSessionRepository workoutSessionRepository;

    @Inject
    ShotEventRepository shotEventRepository;

    @Inject
    CourtCalibrationRepository courtCalibrationRepository;

    @Inject
    PlayerRepository playerRepository;

    @Inject
    WorkoutFrameDataRepository workoutFrameDataRepository;

    @Inject
    PoseAnalysisRepository poseAnalysisRepository;

    @Transactional
    public WorkoutSession createWorkoutSession(UUID playerId, WorkoutSessionRequest request) {
        Player player = playerRepository.findByIdOptional(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found with id: " + playerId));

        // Check if there's an active session
        workoutSessionRepository.findActiveSessionByPlayer(playerId)
                .ifPresent(activeSession -> {
                    throw new IllegalStateException("Player already has an active workout session");
                });

        WorkoutSession session = WorkoutSession.builder()
                .player(player)
                .cameraMode(request.getCameraMode())
                .courtType(request.getCourtType())
                .startTime(OffsetDateTime.now())
                .totalShots(0)
                .madeShots(0)
                .sessionStatus("ACTIVE")
                .calibrationData(request.getCalibrationData())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        workoutSessionRepository.persist(session);
        LOGGER.info("Created new workout session: " + session.getId() + " for player: " + playerId);

        return session;
    }

    @Transactional
    public WorkoutSession endWorkoutSession(UUID sessionId, UUID playerId) {
        WorkoutSession session = workoutSessionRepository.findByIdAndPlayer(sessionId, playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Workout session not found"));

        if (!"ACTIVE".equals(session.getSessionStatus())) {
            throw new IllegalStateException("Session is not active");
        }

        session.setEndTime(OffsetDateTime.now());
        session.setSessionStatus("COMPLETED");
        workoutSessionRepository.persist(session);

        LOGGER.info("Ended workout session: " + sessionId);
        return session;
    }

    @Transactional
    public ShotEvent addShotEvent(UUID sessionId, UUID playerId, ShotEventRequest request) {
        WorkoutSession session = workoutSessionRepository.findByIdAndPlayer(sessionId, playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Workout session not found"));

        if (!"ACTIVE".equals(session.getSessionStatus())) {
            throw new IllegalStateException("Cannot add shots to inactive session");
        }

        ShotEvent shot = ShotEvent.builder()
                .workoutSession(session)
                .timestampMs(request.getTimestampMs())
                .shotResult(request.getShotResult())
                .courtX(request.getCourtX())
                .courtY(request.getCourtY())
                .distanceFromHoop(request.getDistanceFromHoop())
                .releaseAngle(request.getReleaseAngle())
                .releaseVelocity(request.getReleaseVelocity())
                .shotArcHeight(request.getShotArcHeight())
                .videoTimestampMs(request.getVideoTimestampMs())
                .detectionConfidence(request.getDetectionConfidence())
                .trackingData(request.getTrackingData())
                .videoClipPath(request.getVideoClipPath())
                .build();

        shotEventRepository.persist(shot);

        // Update session statistics
        updateSessionStatistics(session);

        LOGGER.info("Added shot event: " + shot.getId() + " to session: " + sessionId);
        return shot;
    }

    @Transactional
    public WorkoutSessionResponse saveCalibration(UUID sessionId, UUID playerId, CalibrationRequest request) {
        WorkoutSession session = workoutSessionRepository.findByIdAndPlayer(sessionId, playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Workout session not found"));

        CourtCalibration calibration = CourtCalibration.builder()
                .workoutSession(session)
                .hoopCenterX(request.getHoopCenterX())
                .hoopCenterY(request.getHoopCenterY())
                .freeThrowLineX(request.getFreeThrowLineX())
                .freeThrowLineY(request.getFreeThrowLineY())
                .threePointLineTopX(request.getThreePointLineTopX())
                .threePointLineTopY(request.getThreePointLineTopY())
                .threePointLineLeftX(request.getThreePointLineLeftX())
                .threePointLineLeftY(request.getThreePointLineLeftY())
                .threePointLineRightX(request.getThreePointLineRightX())
                .threePointLineRightY(request.getThreePointLineRightY())
                .baselineX(request.getBaselineX())
                .baselineY(request.getBaselineY())
                .sidelineLeftX(request.getSidelineLeftX())
                .sidelineLeftY(request.getSidelineLeftY())
                .sidelineRightX(request.getSidelineRightX())
                .sidelineRightY(request.getSidelineRightY())
                .homographyMatrix(request.getHomographyMatrix())
                .calibrationConfidence(request.getCalibrationConfidence())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        courtCalibrationRepository.persist(calibration);

        LOGGER.info("Saved calibration for session: " + sessionId);
        return WorkoutSessionResponse.from(session);
    }

    private void updateSessionStatistics(WorkoutSession session) {
        long totalShots = shotEventRepository.countByWorkoutSession(session.getId());
        long madeShots = shotEventRepository.countByWorkoutSessionAndResult(session.getId(), ShotEvent.ShotResult.MADE);

        session.setTotalShots((int) totalShots);
        session.setMadeShots((int) madeShots);
        workoutSessionRepository.persist(session);
    }

    public WorkoutSessionResponse getSession(UUID sessionId, UUID playerId) {
        WorkoutSession session = workoutSessionRepository.findByIdAndPlayer(sessionId, playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Workout session not found"));

        return WorkoutSessionResponse.from(session);
    }

    public List<WorkoutSessionResponse> getPlayerSessions(UUID playerId) {
        List<WorkoutSession> sessions = workoutSessionRepository.findByPlayer(playerId);
        return sessions.stream()
                .map(WorkoutSessionResponse::from)
                .toList();
    }

    public WorkoutSession getActiveSession(UUID playerId) {
        return workoutSessionRepository.findActiveSessionByPlayer(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("No active workout session found"));
    }

    public List<ShotEvent> getSessionShots(UUID sessionId, UUID playerId) {
        WorkoutSession session = workoutSessionRepository.findByIdAndPlayer(sessionId, playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Workout session not found"));

        return shotEventRepository.findByWorkoutSession(sessionId);
    }

    @Transactional
    public void deleteSession(UUID sessionId, UUID playerId) {
        WorkoutSession session = workoutSessionRepository.findByIdAndPlayer(sessionId, playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Workout session not found"));

        workoutSessionRepository.delete(session);
        LOGGER.info("Deleted workout session: " + sessionId + " for player: " + playerId);
    }

    @Transactional
    public void pauseSession(UUID sessionId, UUID playerId) {
        WorkoutSession session = workoutSessionRepository.findByIdAndPlayer(sessionId, playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Workout session not found"));

        if (!"ACTIVE".equals(session.getSessionStatus())) {
            throw new IllegalStateException("Session is not active");
        }

        session.setSessionStatus("PAUSED");
        workoutSessionRepository.persist(session);
    }

    @Transactional
    public void resumeSession(UUID sessionId, UUID playerId) {
        WorkoutSession session = workoutSessionRepository.findByIdAndPlayer(sessionId, playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Workout session not found"));

        if (!"PAUSED".equals(session.getSessionStatus())) {
            throw new IllegalStateException("Session is not paused");
        }

        session.setSessionStatus("ACTIVE");
        workoutSessionRepository.persist(session);
    }

    @Transactional
    public WorkoutFrameData saveFrameData(UUID sessionId, UUID playerId, FrameDataRequest request) {
        WorkoutSession session = workoutSessionRepository.findByIdAndPlayer(sessionId, playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Workout session not found"));

        if (!"ACTIVE".equals(session.getSessionStatus())) {
            throw new IllegalStateException("Cannot add frame data to inactive session");
        }

        WorkoutFrameData frameData = WorkoutFrameData.builder()
                .session(session)
                .frameTimestamp(request.getFrameTimestamp())
                .ballX(request.getBallX())
                .ballY(request.getBallY())
                .ballConfidence(request.getBallConfidence())
                .hoopX(request.getHoopX())
                .hoopY(request.getHoopY())
                .hoopConfidence(request.getHoopConfidence())
                .poseData(request.getPoseData())
                .trajectoryData(request.getTrajectoryData())
                .ballVelocityX(request.getBallVelocityX())
                .ballVelocityY(request.getBallVelocityY())
                .shotDetected(request.getShotDetected() != null ? request.getShotDetected() : false)
                .createdAt(OffsetDateTime.now())
                .build();

        workoutFrameDataRepository.persist(frameData);
        LOGGER.info("Saved frame data for session: " + sessionId);
        return frameData;
    }

    @Transactional
    public PoseAnalysis savePoseAnalysis(UUID sessionId, UUID playerId, PoseAnalysisRequest request) {
        WorkoutSession session = workoutSessionRepository.findByIdAndPlayer(sessionId, playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Workout session not found"));

        ShotEvent shotEvent = shotEventRepository.findByIdOptional(request.getShotEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Shot event not found"));

        if (!shotEvent.getWorkoutSession().getId().equals(sessionId)) {
            throw new IllegalStateException("Shot event does not belong to this session");
        }

        PoseAnalysis poseAnalysis = PoseAnalysis.builder()
                .shotEvent(shotEvent)
                .elbowAngle(request.getElbowAngle())
                .kneeAngle(request.getKneeAngle())
                .shoulderAngle(request.getShoulderAngle())
                .wristAngle(request.getWristAngle())
                .releaseHeight(request.getReleaseHeight())
                .releaseAngle(request.getReleaseAngle())
                .releaseVelocity(request.getReleaseVelocity())
                .shotSmoothness(request.getShotSmoothness())
                .followThroughScore(request.getFollowThroughScore())
                .balanceScore(request.getBalanceScore())
                .createdAt(OffsetDateTime.now())
                .build();

        poseAnalysisRepository.persist(poseAnalysis);
        LOGGER.info("Saved pose analysis for shot: " + request.getShotEventId());
        return poseAnalysis;
    }

    public RealtimeStatsResponse getRealtimeStats(UUID sessionId, UUID playerId) {
        WorkoutSession session = workoutSessionRepository.findByIdAndPlayer(sessionId, playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Workout session not found"));

        List<ShotEvent> shots = shotEventRepository.findByWorkoutSession(sessionId);
        int totalShots = shots.size();
        long madeShots = shots.stream()
                .filter(s -> s.getShotResult() == ShotEvent.ShotResult.MADE)
                .count();

        BigDecimal fgPercentage = totalShots > 0 
                ? BigDecimal.valueOf(madeShots)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalShots), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal avgReleaseAngle = shots.stream()
                .filter(s -> s.getReleaseAngle() != null)
                .map(ShotEvent::getReleaseAngle)
                .map(BigDecimal::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(
                        shots.stream()
                                .filter(s -> s.getReleaseAngle() != null)
                                .count() > 0 
                                ? BigDecimal.valueOf(shots.stream()
                                        .filter(s -> s.getReleaseAngle() != null)
                                        .count())
                                : BigDecimal.ONE,
                        2, RoundingMode.HALF_UP);

        BigDecimal avgReleaseVelocity = shots.stream()
                .filter(s -> s.getReleaseVelocity() != null)
                .map(ShotEvent::getReleaseVelocity)
                .map(BigDecimal::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(
                        shots.stream()
                                .filter(s -> s.getReleaseVelocity() != null)
                                .count() > 0 
                                ? BigDecimal.valueOf(shots.stream()
                                        .filter(s -> s.getReleaseVelocity() != null)
                                        .count())
                                : BigDecimal.ONE,
                        2, RoundingMode.HALF_UP);

        Map<String, Integer> heatZones = calculateHeatZones(shots);

        List<RealtimeStatsResponse.ShotPosition> recentShots = shots.stream()
                .skip(Math.max(0, shots.size() - 10))
                .map(s -> RealtimeStatsResponse.ShotPosition.builder()
                        .courtX(s.getCourtX())
                        .courtY(s.getCourtY())
                        .result(s.getShotResult() != null ? s.getShotResult().name() : null)
                        .timestamp(s.getTimestampMs())
                        .build())
                .collect(Collectors.toList());

        long sessionDuration = 0;
        if (session.getStartTime() != null) {
            OffsetDateTime endTime = session.getEndTime() != null ? session.getEndTime() : OffsetDateTime.now();
            sessionDuration = Duration.between(session.getStartTime(), endTime).getSeconds();
        }

        return RealtimeStatsResponse.builder()
                .sessionId(sessionId)
                .shotCount(totalShots)
                .fieldGoalPercentage(fgPercentage)
                .shotStreak(calculateShotStreak(shots))
                .releaseAngleAvg(avgReleaseAngle)
                .releaseVelocityAvg(avgReleaseVelocity)
                .heatZones(heatZones)
                .recentShots(recentShots)
                .sessionDuration(sessionDuration)
                .build();
    }

    private Map<String, Integer> calculateHeatZones(List<ShotEvent> shots) {
        Map<String, Integer> zones = new HashMap<>();
        zones.put("PAINT", 0);
        zones.put("MID_RANGE", 0);
        zones.put("CORNER_3", 0);
        zones.put("TOP_3", 0);

        for (ShotEvent shot : shots) {
            if (shot.getDistanceFromHoop() == null) continue;
            
            double distance = shot.getDistanceFromHoop();
            if (distance <= 4.0) {
                zones.put("PAINT", zones.get("PAINT") + 1);
            } else if (distance <= 7.0) {
                zones.put("MID_RANGE", zones.get("MID_RANGE") + 1);
            } else if (distance <= 8.0) {
                zones.put("CORNER_3", zones.get("CORNER_3") + 1);
            } else {
                zones.put("TOP_3", zones.get("TOP_3") + 1);
            }
        }

        return zones;
    }

    private int calculateShotStreak(List<ShotEvent> shots) {
        if (shots.isEmpty()) return 0;
        
        int streak = 0;
        for (int i = shots.size() - 1; i >= 0; i--) {
            if (shots.get(i).getShotResult() == ShotEvent.ShotResult.MADE) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }
}
