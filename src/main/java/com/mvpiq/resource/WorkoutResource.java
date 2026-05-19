package com.mvpiq.resource;

import com.mvpiq.dto.CalibrationRequest;
import com.mvpiq.dto.FrameDataRequest;
import com.mvpiq.dto.FrameDataResponse;
import com.mvpiq.dto.PoseAnalysisRequest;
import com.mvpiq.dto.PoseAnalysisResponse;
import com.mvpiq.dto.RealtimeStatsResponse;
import com.mvpiq.dto.ShotEventRequest;
import com.mvpiq.dto.ShotEventResponse;
import com.mvpiq.dto.WorkoutSessionRequest;
import com.mvpiq.dto.WorkoutSessionResponse;
import com.mvpiq.model.PoseAnalysis;
import com.mvpiq.model.ShotEvent;
import com.mvpiq.model.WorkoutFrameData;
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

    // ── DELETE sessione ────────────────────────────────────────────────────────
    @DELETE
    @Path("/sessions/{sessionId}")
    @RolesAllowed({"PLAYER", "TRAINER"})
    public Response deleteWorkoutSession(
            @PathParam("sessionId") UUID sessionId,
            @QueryParam("userId") UUID userId) {
        workoutService.deleteSession(sessionId, userId);
        return Response.noContent().build();
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
        var session = workoutService.getSession(sessionId, userId);
        return Response.ok(session).build();
    }

    @POST
    @Path("/sessions/{sessionId}/resume")
    @RolesAllowed({"PLAYER", "TRAINER"})
    public Response resumeWorkoutSession(
            @PathParam("sessionId") UUID sessionId,
            @QueryParam("userId") UUID userId) {
        workoutService.resumeSession(sessionId, userId);
        var session = workoutService.getSession(sessionId, userId);
        return Response.ok(session).build();
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
            @Valid CalibrationRequest request) {
        var sessionResponse = workoutService.saveCalibration(sessionId, userId, request);
        return Response.status(Response.Status.CREATED)
                .entity(sessionResponse)
                .build();
    }

    @GET
    @Path("/sessions/active")
    @RolesAllowed({"PLAYER", "TRAINER"})
    public Response getActiveSession(@QueryParam("userId") UUID userId) {
        var session = workoutService.getActiveSession(userId);
        return Response.ok(WorkoutSessionResponse.from(session)).build();
    }

    @POST
    @Path("/sessions/{sessionId}/frames")
    @RolesAllowed({"PLAYER", "TRAINER"})
    public Response saveFrameData(
            @PathParam("sessionId") UUID sessionId,
            @QueryParam("userId") UUID userId,
            @Valid FrameDataRequest request) {
        var frameData = workoutService.saveFrameData(sessionId, userId, request);
        return Response.status(Response.Status.CREATED)
                .entity(FrameDataResponse.from(frameData))
                .build();
    }

    @POST
    @Path("/sessions/{sessionId}/pose-analysis")
    @RolesAllowed({"PLAYER", "TRAINER"})
    public Response savePoseAnalysis(
            @PathParam("sessionId") UUID sessionId,
            @QueryParam("userId") UUID userId,
            @Valid PoseAnalysisRequest request) {
        var poseAnalysis = workoutService.savePoseAnalysis(sessionId, userId, request);
        return Response.status(Response.Status.CREATED)
                .entity(PoseAnalysisResponse.from(poseAnalysis))
                .build();
    }

    @GET
    @Path("/sessions/{sessionId}/realtime-stats")
    @RolesAllowed({"PLAYER", "TRAINER"})
    public Response getRealtimeStats(
            @PathParam("sessionId") UUID sessionId,
            @QueryParam("userId") UUID userId) {
        var stats = workoutService.getRealtimeStats(sessionId, userId);
        return Response.ok(stats).build();
    }
}
