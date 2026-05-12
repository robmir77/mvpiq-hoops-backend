package com.mvpiq.resource;

import com.mvpiq.dto.ShotEventRequest;
import com.mvpiq.dto.ShotEventResponse;
import com.mvpiq.dto.WorkoutSessionRequest;
import com.mvpiq.dto.WorkoutSessionResponse;
import com.mvpiq.model.CourtCalibration;
import com.mvpiq.model.ShotEvent;
import com.mvpiq.service.WorkoutService;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;
import org.jboss.logging.Logger;

@Path("/api/workouts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class WorkoutResource {

    private static final Logger LOGGER = Logger.getLogger(WorkoutResource.class.getName());

    @Inject
    WorkoutService workoutService;

    @POST
    @Path("/sessions")
    @RolesAllowed({"PLAYER", "TRAINER"})
    public Response createWorkoutSession(
            @QueryParam("userId") UUID userId,
            @Valid WorkoutSessionRequest request) {
        var session = workoutService.createWorkoutSession(userId, request);
        return Response.status(Response.Status.CREATED)
                .entity(WorkoutSessionResponse.from(session))
                .build();
    }

    @GET
    @Path("/sessions/{sessionId}")
    @RolesAllowed({"PLAYER", "TRAINER"})
    public Response getWorkoutSession(
            @PathParam("sessionId") UUID sessionId,
            @QueryParam("userId") UUID userId) {
        var session = workoutService.getSession(sessionId, userId);
        return Response.ok(session).build();
    }

    @GET
    @Path("/sessions")
    @RolesAllowed({"PLAYER", "TRAINER"})
    public Response getPlayerWorkoutSessions(@QueryParam("userId") UUID userId) {
        var sessions = workoutService.getPlayerSessions(userId);
        return Response.ok(sessions).build();
    }

    @POST
    @Path("/sessions/{sessionId}/end")
    @RolesAllowed({"PLAYER", "TRAINER"})
    public Response endWorkoutSession(
            @PathParam("sessionId") UUID sessionId,
            @QueryParam("userId") UUID userId) {
        var session = workoutService.endWorkoutSession(sessionId, userId);
        return Response.ok(WorkoutSessionResponse.from(session)).build();
    }

    @POST
    @Path("/sessions/{sessionId}/pause")
    @RolesAllowed({"PLAYER", "TRAINER"})
    public Response pauseWorkoutSession(
            @PathParam("sessionId") UUID sessionId,
            @QueryParam("userId") UUID userId) {
        workoutService.pauseSession(sessionId, userId);
        return Response.ok().build();
    }

    @POST
    @Path("/sessions/{sessionId}/resume")
    @RolesAllowed({"PLAYER", "TRAINER"})
    public Response resumeWorkoutSession(
            @PathParam("sessionId") UUID sessionId,
            @QueryParam("userId") UUID userId) {
        workoutService.resumeSession(sessionId, userId);
        return Response.ok().build();
    }

    @GET
    @Path("/sessions/{sessionId}/shots")
    @RolesAllowed({"PLAYER", "TRAINER"})
    public Response getSessionShots(
            @PathParam("sessionId") UUID sessionId,
            @QueryParam("userId") UUID userId) {
        var shots = workoutService.getSessionShots(sessionId, userId);
        var shotResponses = shots.stream()
                .map(ShotEventResponse::from)
                .toList();
        return Response.ok(shotResponses).build();
    }

    @POST
    @Path("/sessions/{sessionId}/shots")
    @RolesAllowed({"PLAYER", "TRAINER"})
    public Response addShotEvent(
            @PathParam("sessionId") UUID sessionId,
            @QueryParam("userId") UUID userId,
            @Valid ShotEventRequest request) {
        var shot = workoutService.addShotEvent(sessionId, userId, request);
        return Response.status(Response.Status.CREATED)
                .entity(ShotEventResponse.from(shot))
                .build();
    }

    @POST
    @Path("/sessions/{sessionId}/calibration")
    @RolesAllowed({"PLAYER", "TRAINER"})
    public Response saveCalibration(
            @PathParam("sessionId") UUID sessionId,
            @QueryParam("userId") UUID userId,
            @Valid CourtCalibration calibration) {
        var savedCalibration = workoutService.saveCalibration(sessionId, userId, calibration);
        return Response.status(Response.Status.CREATED)
                .entity(savedCalibration)
                .build();
    }

    @GET
    @Path("/sessions/active")
    @RolesAllowed({"PLAYER", "TRAINER"})
    public Response getActiveSession(@QueryParam("userId") UUID userId) {
        var session = workoutService.getActiveSession(userId);
        return Response.ok(WorkoutSessionResponse.from(session)).build();
    }
}
