package com.mvpiq.resource;

import com.mvpiq.dto.*;
import com.mvpiq.service.PlayerCvService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/api/players/{playerId}/cv")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PlayerCvResource {

    @Inject
    PlayerCvService service;

    // ─── GET CV ───────────────────────────────────────────────
    @GET
    @RolesAllowed({"PLAYER", "TRAINER", "SCOUT", "ADMIN"})
    public PlayerCvDTO get(@PathParam("playerId") UUID playerId) {
        return service.getCv(playerId);
    }

    // ─── UPDATE CV ────────────────────────────────────────────
    @PUT
    @RolesAllowed({"PLAYER", "ADMIN"})
    public PlayerCvDTO update(@PathParam("playerId") UUID playerId,
                               PlayerCvDTO dto) {
        return service.updateCv(playerId, dto);
    }

    // ─── ENABLE SHARING ───────────────────────────────────────
    @POST
    @Path("/share")
    @RolesAllowed({"PLAYER", "ADMIN"})
    public CvSharingDTO enableSharing(@PathParam("playerId") UUID playerId) {
        return service.enableSharing(playerId);
    }

    // ─── DISABLE SHARING ──────────────────────────────────────
    @DELETE
    @Path("/share")
    @RolesAllowed({"PLAYER", "ADMIN"})
    public CvSharingDTO disableSharing(@PathParam("playerId") UUID playerId) {
        return service.disableSharing(playerId);
    }

    // ─── GET HIGHLIGHTS ───────────────────────────────────────
    @GET
    @Path("/highlights")
    @RolesAllowed({"PLAYER", "TRAINER", "SCOUT", "ADMIN"})
    public List<PlayerCvHighlightDTO> getHighlights(@PathParam("playerId") UUID playerId) {
        return service.getHighlights(playerId);
    }

    // ─── ADD HIGHLIGHT ────────────────────────────────────────
    // Accetta sia { "externalUrl": "https://youtube.com/..." }
    // che         { "mediaId": "uuid-già-caricato" }
    @POST
    @Path("/highlights")
    @RolesAllowed({"PLAYER", "ADMIN"})
    public Response addHighlight(@PathParam("playerId") UUID playerId,
                                  AddHighlightRequest request) {
        PlayerCvHighlightDTO result = service.addHighlight(playerId, request);
        return Response.status(Response.Status.CREATED).entity(result).build();
    }

    // ─── DELETE HIGHLIGHT ─────────────────────────────────────
    @DELETE
    @Path("/highlights/{highlightId}")
    @RolesAllowed({"PLAYER", "ADMIN"})
    public Response deleteHighlight(@PathParam("playerId") UUID playerId,
                                     @PathParam("highlightId") UUID highlightId) {
        service.deleteHighlight(playerId, highlightId);
        return Response.noContent().build();
    }
}
