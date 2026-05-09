package com.mvpiq.resource;

import com.mvpiq.dto.ApiResponse;
import com.mvpiq.model.Exercise;
import com.mvpiq.service.ExerciseService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Path("/api/exercises")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Slf4j
@Authenticated
public class ExerciseResource {

    @Inject
    ExerciseService exerciseService;

    @POST
    public Response createExercise(Exercise exercise, @QueryParam("ownerId") UUID ownerId, @QueryParam("mediaId") UUID mediaId) {
        try {
            Exercise created = exerciseService.createExercise(exercise, ownerId, mediaId);
            return Response.status(Response.Status.CREATED)
                    .entity(ApiResponse.success(created, "Exercise created successfully"))
                    .build();
        } catch (Exception e) {
            log.error("Error creating exercise", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Error creating exercise: " + e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response updateExercise(@PathParam("id") UUID id, Exercise exercise) {
        try {
            Exercise updated = exerciseService.updateExercise(id, exercise);
            return Response.ok(ApiResponse.success(updated, "Exercise updated successfully")).build();
        } catch (Exception e) {
            log.error("Error updating exercise", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Error updating exercise: " + e.getMessage()))
                    .build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deleteExercise(@PathParam("id") UUID id) {
        try {
            exerciseService.deleteExercise(id);
            return Response.ok(ApiResponse.success("Exercise deleted successfully")).build();
        } catch (Exception e) {
            log.error("Error deleting exercise", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Error deleting exercise: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{id}")
    public Response getExercise(@PathParam("id") UUID id) {
        try {
            Optional<Exercise> exercise = exerciseService.getExercise(id);
            if (exercise.isPresent()) {
                return Response.ok(ApiResponse.success(exercise.get(), "Exercise retrieved successfully")).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(ApiResponse.error("Exercise not found"))
                        .build();
            }
        } catch (Exception e) {
            log.error("Error retrieving exercise", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error retrieving exercise: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/owner/{ownerId}")
    public Response getExercisesByOwner(@PathParam("ownerId") UUID ownerId) {
        try {
            List<Exercise> exercises = exerciseService.getExercisesByOwner(ownerId);
            return Response.ok(ApiResponse.success(exercises, "Exercises retrieved successfully")).build();
        } catch (Exception e) {
            log.error("Error retrieving exercises by owner", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error retrieving exercises: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/public")
    public Response getPublicExercises() {
        try {
            List<Exercise> exercises = exerciseService.getPublicExercises();
            return Response.ok(ApiResponse.success(exercises, "Public exercises retrieved successfully")).build();
        } catch (Exception e) {
            log.error("Error retrieving public exercises", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error retrieving exercises: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/category/{category}")
    public Response getExercisesByCategory(@PathParam("category") String category) {
        try {
            List<Exercise> exercises = exerciseService.getExercisesByCategory(category);
            return Response.ok(ApiResponse.success(exercises, "Exercises retrieved successfully")).build();
        } catch (Exception e) {
            log.error("Error retrieving exercises by category", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error retrieving exercises: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/difficulty/{difficulty}")
    public Response getExercisesByDifficulty(@PathParam("difficulty") String difficulty) {
        try {
            List<Exercise> exercises = exerciseService.getExercisesByDifficulty(difficulty);
            return Response.ok(ApiResponse.success(exercises, "Exercises retrieved successfully")).build();
        } catch (Exception e) {
            log.error("Error retrieving exercises by difficulty", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error retrieving exercises: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/search")
    public Response searchExercises(@QueryParam("title") String title) {
        try {
            List<Exercise> exercises = exerciseService.searchExercises(title);
            return Response.ok(ApiResponse.success(exercises, "Exercises retrieved successfully")).build();
        } catch (Exception e) {
            log.error("Error searching exercises", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error searching exercises: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/media-type/{mediaType}")
    public Response getExercisesByMediaType(@PathParam("mediaType") String mediaType) {
        try {
            List<Exercise> exercises = exerciseService.getExercisesByMediaType(mediaType);
            return Response.ok(ApiResponse.success(exercises, "Exercises retrieved successfully")).build();
        } catch (Exception e) {
            log.error("Error retrieving exercises by media type", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error retrieving exercises: " + e.getMessage()))
                    .build();
        }
    }
}
