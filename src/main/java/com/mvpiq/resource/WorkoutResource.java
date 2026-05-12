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
import java.util.logging.Logger;

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
        
        try {
            var session = workoutService.createWorkoutSession(userId, request);
            return Response.status(Response.Status.CREATED)
                    .entity(WorkoutSessionResponse.from(session))
                    .build();
        } catch (Exception e) {
            LOGGER.severe("Error creating workout session: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/sessions/{sessionId}")
    @RolesAllowed({"PLAYER", "TRAINER"})
    public Response getWorkoutSession(
            @PathParam("sessionId") UUID sessionId,
            @QueryParam("userId") UUID userId) {
        
        try {
            var session = workoutService.getSession(sessionId, userId);
            return Response.ok(session).build();
        } catch (Exception e) {
            LOGGER.severe("Error getting workout session: " + e.getMessage());
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/sessions")
    @RolesAllowed({"PLAYER", "TRAINER"})
    public Response getPlayerWorkoutSessions(@QueryParam("userId") UUID userId) {
        
        try {
            var sessions = workoutService.getPlayerSessions(userId);
            return Response.ok(sessions).build();
        } catch (Exception e) {
            LOGGER.severe("Error getting player workout sessions: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    @POST
    @Path("/sessions/{sessionId}/end")
    @RolesAllowed({"PLAYER", "TRAINER"})
    public Response endWorkoutSession(
            @PathParam("sessionId") UUID sessionId,
            @QueryParam("userId") UUID userId) {
        
        try {
            var session = workoutService.endWorkoutSession(sessionId, userId);
            return Response.ok(WorkoutSessionResponse.from(session)).build();
        } catch (Exception e) {
            LOGGER.severe("Error ending workout session: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    @POST
    @Path("/sessions/{sessionId}/pause")
    @RolesAllowed({"PLAYER", "TRAINER"})
    public Response pauseWorkoutSession(
            @PathParam("sessionId") UUID sessionId,
            @QueryParam("userId") UUID userId) {
        
        try {
            workoutService.pauseSession(sessionId, userId);
            return Response.ok().build();
        } catch (Exception e) {
            LOGGER.severe("Error pausing workout session: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    @POST
    @Path("/sessions/{sessionId}/resume")
    @RolesAllowed({"PLAYER", "TRAINER"})
    public Response resumeWorkoutSession(
            @PathParam("sessionId") UUID sessionId,
            @QueryParam("userId") UUID userId) {
        
        try {
            workoutService.resumeSession(sessionId, userId);
            return Response.ok().build();
        } catch (Exception e) {
            LOGGER.severe("Error resuming workout session: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/sessions/{sessionId}/shots")
    @RolesAllowed({"PLAYER", "TRAINER"})
    public Response getSessionShots(
            @PathParam("sessionId") UUID sessionId,
            @QueryParam("userId") UUID userId) {
        
        try {
            var shots = workoutService.getSessionShots(sessionId, userId);
            var shotResponses = shots.stream()
                    .map(ShotEventResponse::from)
                    .toList();
            return Response.ok(shotResponses).build();
        } catch (Exception e) {
            LOGGER.severe("Error getting session shots: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    @POST
    @Path("/sessions/{sessionId}/shots")
    @RolesAllowed({"PLAYER", "TRAINER"})
    public Response addShotEvent(
            @PathParam("sessionId") UUID sessionId,
            @QueryParam("userId") UUID userId,
            @Valid ShotEventRequest request) {
        
        try {
            var shot = workoutService.addShotEvent(sessionId, userId, request);
            return Response.status(Response.Status.CREATED)
                    .entity(ShotEventResponse.from(shot))
                    .build();
        } catch (Exception e) {
            LOGGER.severe("Error adding shot event: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    @POST
    @Path("/sessions/{sessionId}/calibration")
    @RolesAllowed({"PLAYER", "TRAINER"})
    public Response saveCalibration(
            @PathParam("sessionId") UUID sessionId,
            @QueryParam("userId") UUID userId,
            @Valid CourtCalibration calibration) {
        
        try {
            var savedCalibration = workoutService.saveCalibration(sessionId, userId, calibration);
            return Response.status(Response.Status.CREATED)
                    .entity(savedCalibration)
                    .build();
        } catch (Exception e) {
            LOGGER.severe("Error saving calibration: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/sessions/active")
    @RolesAllowed({"PLAYER", "TRAINER"})
    public Response getActiveSession(@QueryParam("userId") UUID userId) {
        
        try {
            var session = workoutService.getActiveSession(userId);
            return Response.ok(WorkoutSessionResponse.from(session)).build();
        } catch (Exception e) {
            LOGGER.severe("Error getting active session: " + e.getMessage());
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }
}
