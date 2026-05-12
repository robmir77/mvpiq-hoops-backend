package com.mvpiq.resource;

import com.mvpiq.dto.ShotChartResponse;
import com.mvpiq.model.ShotEvent;
import com.mvpiq.service.ShotAnalyticsService;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Path("/api/workouts/{sessionId}/analytics")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class ShotAnalyticsResource {

    private static final Logger LOGGER = Logger.getLogger(ShotAnalyticsResource.class.getName());

    @Inject
    ShotAnalyticsService analyticsService;

    @GET
    @Path("/shot-chart")
    @RolesAllowed({"PLAYER", "TRAINER"})
    public Response getShotChart(
            @PathParam("sessionId") UUID sessionId,
            @QueryParam("userId") UUID userId) {
        
        try {
            var shotChart = analyticsService.getShotChart(sessionId, userId);
            return Response.ok(shotChart).build();
        } catch (Exception e) {
            LOGGER.severe("Error getting shot chart: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/stats")
    @RolesAllowed({"PLAYER", "TRAINER"})
    public Response getSessionStatistics(
            @PathParam("sessionId") UUID sessionId,
            @QueryParam("userId") UUID userId) {
        
        try {
            var shotChart = analyticsService.getShotChart(sessionId, userId);
            return Response.ok(shotChart.getSessionStats()).build();
        } catch (Exception e) {
            LOGGER.severe("Error getting session statistics: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/zones")
    @RolesAllowed({"PLAYER", "TRAINER"})
    public Response getZoneStatistics(
            @PathParam("sessionId") UUID sessionId,
            @QueryParam("userId") UUID userId) {
        
        try {
            var shotChart = analyticsService.getShotChart(sessionId, userId);
            return Response.ok(shotChart.getZoneStats()).build();
        } catch (Exception e) {
            LOGGER.severe("Error getting zone statistics: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/hot-zones")
    @RolesAllowed({"PLAYER", "TRAINER"})
    public Response getHotZones(
            @PathParam("sessionId") UUID sessionId,
            @QueryParam("userId") UUID userId,
            @DefaultValue("10") @QueryParam("limit") int limit) {
        
        try {
            var hotShots = analyticsService.getHotZones(userId, limit);
            var hotShotResponses = hotShots.stream()
                    .map(shot -> {
                        var response = new ShotChartResponse.ShotPoint();
                        response.setX(shot.getCourtX());
                        response.setY(shot.getCourtY());
                        response.setMade(true);
                        response.setDistance(shot.getDistanceFromHoop());
                        response.setZone(determineZone(shot.getDistanceFromHoop()));
                        return response;
                    })
                    .collect(Collectors.toList());
            
            return Response.ok(hotShotResponses).build();
        } catch (Exception e) {
            LOGGER.severe("Error getting hot zones: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/cold-zones")
    @RolesAllowed({"PLAYER", "TRAINER"})
    public Response getColdZones(
            @PathParam("sessionId") UUID sessionId,
            @QueryParam("userId") UUID userId,
            @DefaultValue("10") @QueryParam("limit") int limit) {
        
        try {
            var coldShots = analyticsService.getColdZones(userId, limit);
            var coldShotResponses = coldShots.stream()
                    .map(shot -> {
                        var response = new ShotChartResponse.ShotPoint();
                        response.setX(shot.getCourtX());
                        response.setY(shot.getCourtY());
                        response.setMade(false);
                        response.setDistance(shot.getDistanceFromHoop());
                        response.setZone(determineZone(shot.getDistanceFromHoop()));
                        return response;
                    })
                    .collect(Collectors.toList());
            
            return Response.ok(coldShotResponses).build();
        } catch (Exception e) {
            LOGGER.severe("Error getting cold zones: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/career-stats")
    @RolesAllowed({"PLAYER", "TRAINER"})
    public Response getCareerStats(@QueryParam("userId") UUID userId) {
        
        try {
            var careerStats = analyticsService.getPlayerCareerStats(userId);
            return Response.ok(careerStats).build();
        } catch (Exception e) {
            LOGGER.severe("Error getting career stats: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    private String determineZone(Double distance) {
        if (distance == null) return "UNKNOWN";
        if (distance <= 4.0) return "PAINT";
        if (distance <= 7.0) return "MID_RANGE";
        if (distance <= 8.0) return "CORNER";
        return "THREE_POINT";
    }
}
