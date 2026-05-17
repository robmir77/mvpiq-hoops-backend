package com.mvpiq.resource;

import com.mvpiq.dto.CareerStatsDTO;
import com.mvpiq.service.ShotAnalyticsService;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

/**
 * Endpoint analytics a livello di player (non di sessione specifica).
 * Path: /api/workouts/player/analytics/...
 *
 * Separato da ShotAnalyticsResource che usa /api/workouts/{sessionId}/analytics/...
 * Il FE chiama: GET /api/workouts/player/analytics/career-stats?userId={id}
 */
@Path("/api/workouts/player/analytics")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class PlayerAnalyticsResource {

    @Inject
    ShotAnalyticsService analyticsService;

    @GET
    @Path("/career-stats")
    @RolesAllowed({"PLAYER", "TRAINER"})
    public Response getCareerStats(@QueryParam("userId") UUID userId) {
        if (userId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("userId is required")
                    .build();
        }
        CareerStatsDTO careerStats = analyticsService.getPlayerCareerStats(userId);
        return Response.ok(careerStats).build();
    }
}
