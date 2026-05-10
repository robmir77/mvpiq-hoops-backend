package com.mvpiq.resource;

import com.mvpiq.dto.ApiResponse;
import com.mvpiq.service.NavigationService;
import io.quarkus.security.Authenticated;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/api")
@RequestScoped
@Authenticated
public class NavigationResource {

    @Inject
    NavigationService navigationService;

    /**
     * Restituisce le sezioni dell'applicazione accessibili all'utente corrente
     */
    @GET
    @Path("/navigation/sections")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAccessibleSections() {
        try {
            var sections = navigationService.getAccessibleSections();
            ApiResponse<List<NavigationService.NavigationItem>> response = ApiResponse.success(sections, "Navigation sections retrieved successfully");
            return Response.ok(response).build();
        } catch (Exception e) {
            ApiResponse<Void> errorResponse = ApiResponse.error("Failed to retrieve navigation sections: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorResponse).build();
        }
    }

    /**
     * Verifica se l'utente corrente può accedere a una specifica sezione
     */
    @GET
    @Path("/navigation/sections/{sectionId}/access")
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkSectionAccess(@PathParam("sectionId") String sectionId) {
        try {
            boolean canAccess = navigationService.canAccessSection(sectionId);
            
            ApiResponse<Boolean> response = ApiResponse.success(canAccess, 
                canAccess ? "Access granted" : "Access denied");
            
            return Response.ok(response).build();
        } catch (Exception e) {
            ApiResponse<Void> errorResponse = ApiResponse.error("Failed to check section access: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorResponse).build();
        }
    }

    /**
     * Restituisce tutte le sezioni disponibili (per admin/debug)
     */
    @GET
    @Path("/navigation/sections/all")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllSections() {
        try {
            var sections = navigationService.getAllSections();
            ApiResponse<List<NavigationService.NavigationItem>> response = ApiResponse.success(sections, "All navigation sections retrieved successfully");
            return Response.ok(response).build();
        } catch (Exception e) {
            ApiResponse<Void> errorResponse = ApiResponse.error("Failed to retrieve all sections: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorResponse).build();
        }
    }
}
