package com.mvpiq.resource;

import com.mvpiq.dto.ApiResponse;
import com.mvpiq.model.PlayerProfile;
import com.mvpiq.service.PlayerProfileService;
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

@Path("/api/player-profiles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Slf4j
@Authenticated
public class PlayerProfileResource {

    @Inject
    PlayerProfileService playerProfileService;

    @POST
    public Response createPlayerProfile(PlayerProfile profile, @QueryParam("userId") UUID userId) {
        try {
            PlayerProfile created = playerProfileService.createPlayerProfile(profile, userId);
            return Response.status(Response.Status.CREATED)
                    .entity(ApiResponse.success(created, "Player profile created successfully"))
                    .build();
        } catch (Exception e) {
            log.error("Error creating player profile", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Error creating player profile: " + e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response updatePlayerProfile(@PathParam("id") UUID id, PlayerProfile profile) {
        try {
            PlayerProfile updated = playerProfileService.updatePlayerProfile(id, profile);
            return Response.ok(ApiResponse.success(updated, "Player profile updated successfully")).build();
        } catch (Exception e) {
            log.error("Error updating player profile", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Error updating player profile: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{id}")
    public Response getPlayerProfile(@PathParam("id") UUID id) {
        try {
            Optional<PlayerProfile> profile = playerProfileService.getPlayerProfile(id);
            if (profile.isPresent()) {
                return Response.ok(ApiResponse.success(profile.get(), "Player profile retrieved successfully")).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(ApiResponse.error("Player profile not found"))
                        .build();
            }
        } catch (Exception e) {
            log.error("Error retrieving player profile", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error retrieving player profile: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/user/{userId}")
    public Response getPlayerProfileByUserId(@PathParam("userId") UUID userId) {
        try {
            Optional<PlayerProfile> profile = playerProfileService.getPlayerProfileByUserId(userId);
            if (profile.isPresent()) {
                return Response.ok(ApiResponse.success(profile.get(), "Player profile retrieved successfully")).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(ApiResponse.error("Player profile not found"))
                        .build();
            }
        } catch (Exception e) {
            log.error("Error retrieving player profile by user ID", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error retrieving player profile: " + e.getMessage()))
                    .build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deletePlayerProfile(@PathParam("id") UUID id) {
        try {
            playerProfileService.deletePlayerProfile(id);
            return Response.ok(ApiResponse.success("Player profile deleted successfully")).build();
        } catch (Exception e) {
            log.error("Error deleting player profile", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Error deleting player profile: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/country/{country}")
    public Response getPlayersByCountry(@PathParam("country") String country) {
        try {
            List<PlayerProfile> players = playerProfileService.getPlayersByCountry(country);
            return Response.ok(ApiResponse.success(players, "Players retrieved successfully")).build();
        } catch (Exception e) {
            log.error("Error retrieving players by country", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error retrieving players: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/level/{level}")
    public Response getPlayersByLevel(@PathParam("level") String level) {
        try {
            List<PlayerProfile> players = playerProfileService.getPlayersByLevel(level);
            return Response.ok(ApiResponse.success(players, "Players retrieved successfully")).build();
        } catch (Exception e) {
            log.error("Error retrieving players by level", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error retrieving players: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/position/{position}")
    public Response getPlayersByPosition(@PathParam("position") String position) {
        try {
            List<PlayerProfile> players = playerProfileService.getPlayersByPosition(position);
            return Response.ok(ApiResponse.success(players, "Players retrieved successfully")).build();
        } catch (Exception e) {
            log.error("Error retrieving players by position", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error retrieving players: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/age-range")
    public Response getPlayersByAgeRange(@QueryParam("minAge") int minAge, @QueryParam("maxAge") int maxAge) {
        try {
            List<PlayerProfile> players = playerProfileService.getPlayersByAgeRange(minAge, maxAge);
            return Response.ok(ApiResponse.success(players, "Players retrieved successfully")).build();
        } catch (Exception e) {
            log.error("Error retrieving players by age range", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error retrieving players: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/public")
    public Response getPublicPlayerProfiles() {
        try {
            List<PlayerProfile> players = playerProfileService.getPublicPlayerProfiles();
            return Response.ok(ApiResponse.success(players, "Public player profiles retrieved successfully")).build();
        } catch (Exception e) {
            log.error("Error retrieving public player profiles", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error retrieving player profiles: " + e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/search")
    public Response searchPlayers(Map<String, Object> filters) {
        try {
            List<PlayerProfile> players = playerProfileService.searchPlayers(filters);
            return Response.ok(ApiResponse.success(players, "Players retrieved successfully")).build();
        } catch (Exception e) {
            log.error("Error searching players", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Error searching players: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{id}/stats")
    public Response getPlayerStats(@PathParam("id") UUID id) {
        try {
            Map<String, Object> stats = playerProfileService.getPlayerStats(id);
            return Response.ok(ApiResponse.success(stats, "Player stats retrieved successfully")).build();
        } catch (Exception e) {
            log.error("Error retrieving player stats", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error retrieving player stats: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{id}/rankings")
    public Response getPlayerRankings(@PathParam("id") UUID id) {
        try {
            List<Map<String, Object>> rankings = playerProfileService.getPlayerRankings(id);
            return Response.ok(ApiResponse.success(rankings, "Player rankings retrieved successfully")).build();
        } catch (Exception e) {
            log.error("Error retrieving player rankings", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error retrieving player rankings: " + e.getMessage()))
                    .build();
        }
    }
}
