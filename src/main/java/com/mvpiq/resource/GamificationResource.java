package com.mvpiq.resource;

import com.mvpiq.dto.ApiResponse;
import com.mvpiq.model.AthleteBadge;
import com.mvpiq.repositories.AthleteBadgeRepository;
import com.mvpiq.service.GamificationEngine;
import com.mvpiq.service.AchievementService;
import com.mvpiq.service.ProgressTrackingService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/api/players/{playerId}/gamification")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GamificationResource {

    @Inject
    GamificationEngine gamificationEngine;
    
    @Inject
    AchievementService achievementService;
    
    @Inject
    ProgressTrackingService progressTrackingService;
    
    @Inject
    AthleteBadgeRepository athleteBadgeRepository;

    /**
     * Controlla e aggiorna gli achievement di un giocatore
     */
    @POST
    @Path("/achievements/check")
    public Response checkAchievements(@PathParam("playerId") UUID playerId) {
        try {
            achievementService.checkAndAwardAchievements(playerId, "MANUAL_CHECK");
            return Response.ok(ApiResponse.success(null, "Achievements checked successfully")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error checking achievements: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Ottiene tutti i badge di un giocatore
     */
    @GET
    @Path("/badges")
    public Response getPlayerBadges(@PathParam("playerId") UUID playerId) {
        try {
            List<AthleteBadge> badges = achievementService.getUserBadges(playerId);
            return Response.ok(ApiResponse.success(badges, "Player badges retrieved successfully")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error retrieving badges: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Assegna un badge specifico a un giocatore
     */
    @POST
    @Path("/badges/{badgeId}/award")
    public Response awardBadge(@PathParam("playerId") UUID playerId, @PathParam("badgeId") UUID badgeId) {
        try {
            achievementService.awardBadge(playerId, badgeId);
            return Response.ok(ApiResponse.success(null, "Badge awarded successfully")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error awarding badge: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Ottiene lo streak corrente di un giocatore
     */
    @GET
    @Path("/streak")
    public Response getCurrentStreak(@PathParam("playerId") UUID playerId) {
        try {
            gamificationEngine.checkStreak(playerId);
            // In una implementazione reale, qui restituiremmo il valore dello streak
            return Response.ok(ApiResponse.success(null, "Streak checked successfully")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error checking streak: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Calcola il progresso giornaliero
     */
    @POST
    @Path("/daily-progress")
    public Response calculateDailyProgress(@PathParam("playerId") UUID playerId) {
        try {
            progressTrackingService.trackDailyProgress(playerId);
            return Response.ok(ApiResponse.success(null, "Daily progress calculated successfully")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error calculating daily progress: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Ottiene le statistiche settimanali
     */
    @GET
    @Path("/weekly-stats")
    public Response getWeeklyStats(@PathParam("playerId") UUID playerId) {
        try {
            ProgressTrackingService.WeeklyProgressStats stats = progressTrackingService.getWeeklyStats(playerId);
            return Response.ok(ApiResponse.success(stats, "Weekly stats retrieved successfully")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error retrieving weekly stats: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Ottiene le statistiche mensili
     */
    @GET
    @Path("/monthly-stats")
    public Response getMonthlyStats(@PathParam("playerId") UUID playerId) {
        try {
            ProgressTrackingService.MonthlyProgressStats stats = progressTrackingService.getMonthlyStats(playerId);
            return Response.ok(ApiResponse.success(stats, "Monthly stats retrieved successfully")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error retrieving monthly stats: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Ottiene il riepilogo del progresso totale
     */
    @GET
    @Path("/progress-summary")
    public Response getProgressSummary(@PathParam("playerId") UUID playerId) {
        try {
            ProgressTrackingService.ProgressSummary summary = progressTrackingService.getProgressSummary(playerId);
            return Response.ok(ApiResponse.success(summary, "Progress summary retrieved successfully")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error retrieving progress summary: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Ottiene il progresso verso un goal specifico
     */
    @GET
    @Path("/goals/{goalId}/progress")
    public Response getGoalProgress(@PathParam("playerId") UUID playerId, @PathParam("goalId") UUID goalId) {
        try {
            ProgressTrackingService.GoalProgress progress = progressTrackingService.getGoalProgress(goalId);
            if (progress == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(ApiResponse.error("Goal not found"))
                        .build();
            }
            return Response.ok(ApiResponse.success(progress, "Goal progress retrieved successfully")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error retrieving goal progress: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Aggiorna il progresso di un goal
     */
    @PUT
    @Path("/goals/{goalId}/progress")
    public Response updateGoalProgress(@PathParam("playerId") UUID playerId, 
                                     @PathParam("goalId") UUID goalId, 
                                     double newValue) {
        try {
            progressTrackingService.updateGoalProgress(goalId, newValue);
            return Response.ok(ApiResponse.success(null, "Goal progress updated successfully")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error updating goal progress: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Aggiorna i punti di un giocatore
     */
    @POST
    @Path("/points/update")
    public Response updatePlayerPoints(@PathParam("playerId") UUID playerId) {
        try {
            gamificationEngine.updatePlayerPoints(playerId);
            return Response.ok(ApiResponse.success(null, "Player points updated successfully")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error updating player points: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Inizializza i badge di sistema
     */
    @POST
    @Path("/achievements/initialize")
    public Response initializeSystemBadges() {
        try {
            achievementService.initializeSystemBadges();
            return Response.ok(ApiResponse.success(null, "System badges initialized successfully")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error initializing system badges: " + e.getMessage()))
                    .build();
        }
    }
}
