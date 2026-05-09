package com.mvpiq.resource;

import com.mvpiq.dto.ApiResponse;
import com.mvpiq.model.ScoutSavedFilter;
import com.mvpiq.service.ScoutService;
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

@Path("/api/scout")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Slf4j
@Authenticated
public class ScoutResource {

    @Inject
    ScoutService scoutService;

    @POST
    @Path("/filters")
    public Response createSavedFilter(Map<String, Object> request) {
        try {
            UUID scoutId = UUID.fromString(request.get("scoutId").toString());
            String name = (String) request.get("name");
            @SuppressWarnings("unchecked")
            Map<String, Object> filterJson = (Map<String, Object>) request.get("filterJson");
            
            ScoutSavedFilter filter = scoutService.createSavedFilter(scoutId, name, filterJson);
            return Response.status(Response.Status.CREATED)
                    .entity(ApiResponse.success(filter, "Filter saved successfully"))
                    .build();
        } catch (Exception e) {
            log.error("Error saving filter", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Error saving filter: " + e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/filters/{id}")
    public Response updateSavedFilter(@PathParam("id") UUID id, Map<String, Object> request) {
        try {
            String name = (String) request.get("name");
            @SuppressWarnings("unchecked")
            Map<String, Object> filterJson = (Map<String, Object>) request.get("filterJson");
            
            ScoutSavedFilter filter = scoutService.updateSavedFilter(id, name, filterJson);
            return Response.ok(ApiResponse.success(filter, "Filter updated successfully")).build();
        } catch (Exception e) {
            log.error("Error updating filter", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Error updating filter: " + e.getMessage()))
                    .build();
        }
    }

    @DELETE
    @Path("/filters/{id}")
    public Response deleteSavedFilter(@PathParam("id") UUID id) {
        try {
            scoutService.deleteSavedFilter(id);
            return Response.ok(ApiResponse.success("Filter deleted successfully")).build();
        } catch (Exception e) {
            log.error("Error deleting filter", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Error deleting filter: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/filters/{id}")
    public Response getSavedFilter(@PathParam("id") UUID id) {
        try {
            Optional<ScoutSavedFilter> filter = scoutService.getSavedFilter(id);
            if (filter.isPresent()) {
                return Response.ok(ApiResponse.success(filter.get(), "Filter retrieved successfully")).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(ApiResponse.error("Filter not found"))
                        .build();
            }
        } catch (Exception e) {
            log.error("Error retrieving filter", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error retrieving filter: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/filters/scout/{scoutId}")
    public Response getScoutSavedFilters(@PathParam("scoutId") UUID scoutId) {
        try {
            List<ScoutSavedFilter> filters = scoutService.getScoutSavedFilters(scoutId);
            return Response.ok(ApiResponse.success(filters, "Filters retrieved successfully")).build();
        } catch (Exception e) {
            log.error("Error retrieving scout filters", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error retrieving filters: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/filters/search")
    public Response searchSavedFilters(@QueryParam("scoutId") UUID scoutId, @QueryParam("name") String name) {
        try {
            List<ScoutSavedFilter> filters = scoutService.searchSavedFilters(scoutId, name);
            return Response.ok(ApiResponse.success(filters, "Filters retrieved successfully")).build();
        } catch (Exception e) {
            log.error("Error searching filters", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error searching filters: " + e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/search")
    public Response searchPlayers(Map<String, Object> filters) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = scoutService.searchPlayers(filters);
            return Response.ok(ApiResponse.success(results, "Players retrieved successfully")).build();
        } catch (Exception e) {
            log.error("Error searching players", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Error searching players: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/rankings")
    public Response getPlayerRankings(@QueryParam("scope") String scope, @QueryParam("scopeValue") String scopeValue) {
        try {
            List<Map<String, Object>> rankings = scoutService.getPlayerRankings(scope, scopeValue);
            return Response.ok(ApiResponse.success(rankings, "Rankings retrieved successfully")).build();
        } catch (Exception e) {
            log.error("Error retrieving rankings", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error retrieving rankings: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/players/{playerId}/profile")
    public Response getPlayerScoutProfile(@PathParam("playerId") UUID playerId) {
        try {
            Optional<Map<String, Object>> profile = scoutService.getPlayerScoutProfile(playerId);
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
}
