package com.mvpiq.resource;

import com.mvpiq.service.TrainingService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

@Path("/api/training")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TrainingResource {

    @Inject
    TrainingService trainingService;

    @GET
    @Path("/programs")
    public Response getPrograms() {
        return Response.ok(trainingService.getPublicPrograms()).build();
    }

    @GET
    @Path("/sessions/{userId}")
    public Response getUserSessions(@PathParam("userId") UUID userId) {
        return Response.ok(trainingService.getUserSessions(userId)).build();
    }

    @GET
    @Path("/stats/{userId}")
    public Response getUserStats(@PathParam("userId") UUID userId) {
        return Response.ok(trainingService.getUserStats(userId)).build();
    }
}
