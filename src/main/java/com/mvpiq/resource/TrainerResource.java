package com.mvpiq.resource;

import com.mvpiq.dto.ApiResponse;
import com.mvpiq.model.TrainerFollow;
import com.mvpiq.service.TrainerService;
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

@Path("/api/trainer")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Slf4j
@Authenticated
public class TrainerResource {

    @Inject
    TrainerService trainerService;

    @POST
    @Path("/follow")
    public Response followPlayer(Map<String, Object> request) {
        try {
            UUID trainerId = UUID.fromString(request.get("trainerId").toString());
            UUID playerId = UUID.fromString(request.get("playerId").toString());
            
            TrainerFollow follow = trainerService.followPlayer(trainerId, playerId);
            return Response.status(Response.Status.CREATED)
                    .entity(ApiResponse.success(follow, "Player followed successfully"))
                    .build();
        } catch (Exception e) {
            log.error("Error following player", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Error following player: " + e.getMessage()))
                    .build();
        }
    }

    @DELETE
    @Path("/follow")
    public Response unfollowPlayer(@QueryParam("trainerId") UUID trainerId, @QueryParam("playerId") UUID playerId) {
        try {
            trainerService.unfollowPlayer(trainerId, playerId);
            return Response.ok(ApiResponse.success("Player unfollowed successfully")).build();
        } catch (Exception e) {
            log.error("Error unfollowing player", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Error unfollowing player: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/follows/{trainerId}")
    public Response getTrainerFollows(@PathParam("trainerId") UUID trainerId) {
        try {
            List<TrainerFollow> follows = trainerService.getTrainerFollows(trainerId);
            return Response.ok(ApiResponse.success(follows, "Trainer follows retrieved successfully")).build();
        } catch (Exception e) {
            log.error("Error retrieving trainer follows", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error retrieving follows: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/followers/{playerId}")
    public Response getPlayerFollowers(@PathParam("playerId") UUID playerId) {
        try {
            List<TrainerFollow> followers = trainerService.getPlayerFollowers(playerId);
            return Response.ok(ApiResponse.success(followers, "Player followers retrieved successfully")).build();
        } catch (Exception e) {
            log.error("Error retrieving player followers", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error retrieving followers: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/follow/check")
    public Response isFollowingPlayer(@QueryParam("trainerId") UUID trainerId, @QueryParam("playerId") UUID playerId) {
        try {
            boolean isFollowing = trainerService.isFollowingPlayer(trainerId, playerId);
            return Response.ok(ApiResponse.success(Map.of("isFollowing", isFollowing), "Follow status checked successfully")).build();
        } catch (Exception e) {
            log.error("Error checking follow status", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error checking follow status: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/stats/{trainerId}")
    public Response getTrainerStats(@PathParam("trainerId") UUID trainerId) {
        try {
            Map<String, Object> stats = trainerService.getTrainerStats(trainerId);
            return Response.ok(ApiResponse.success(stats, "Trainer stats retrieved successfully")).build();
        } catch (Exception e) {
            log.error("Error retrieving trainer stats", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error retrieving stats: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/players/{trainerId}/progress")
    public Response getTrainerPlayersProgress(@PathParam("trainerId") UUID trainerId) {
        try {
            List<Map<String, Object>> progress = trainerService.getTrainerPlayersProgress(trainerId);
            return Response.ok(ApiResponse.success(progress, "Players progress retrieved successfully")).build();
        } catch (Exception e) {
            log.error("Error retrieving players progress", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error retrieving progress: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/players/{playerId}/details")
    public Response getPlayerDetailsForTrainer(@PathParam("playerId") UUID playerId, @QueryParam("trainerId") UUID trainerId) {
        try {
            Optional<Map<String, Object>> details = trainerService.getPlayerDetailsForTrainer(trainerId, playerId);
            if (details.isPresent()) {
                return Response.ok(ApiResponse.success(details.get(), "Player details retrieved successfully")).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(ApiResponse.error("Player details not found"))
                        .build();
            }
        } catch (Exception e) {
            log.error("Error retrieving player details", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error retrieving player details: " + e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/feedback")
    public Response addPlayerFeedback(Map<String, Object> request) {
        try {
            UUID trainerId = UUID.fromString(request.get("trainerId").toString());
            UUID playerId = UUID.fromString(request.get("playerId").toString());
            String feedback = (String) request.get("feedback");
            
            trainerService.addPlayerFeedback(trainerId, playerId, feedback);
            return Response.ok(ApiResponse.success("Feedback added successfully")).build();
        } catch (Exception e) {
            log.error("Error adding feedback", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Error adding feedback: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/follow-count/{trainerId}")
    public Response getTrainerFollowCount(@PathParam("trainerId") UUID trainerId) {
        try {
            long count = trainerService.getTrainerFollowCount(trainerId);
            return Response.ok(ApiResponse.success(Map.of("count", count), "Follow count retrieved successfully")).build();
        } catch (Exception e) {
            log.error("Error retrieving follow count", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error retrieving follow count: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/follower-count/{playerId}")
    public Response getPlayerFollowerCount(@PathParam("playerId") UUID playerId) {
        try {
            long count = trainerService.getPlayerFollowerCount(playerId);
            return Response.ok(ApiResponse.success(Map.of("count", count), "Follower count retrieved successfully")).build();
        } catch (Exception e) {
            log.error("Error retrieving follower count", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error retrieving follower count: " + e.getMessage()))
                    .build();
        }
    }
}
