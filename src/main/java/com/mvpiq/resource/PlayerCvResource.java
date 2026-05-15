package com.mvpiq.resource;

import com.mvpiq.dto.PlayerCvDTO;
import com.mvpiq.model.PlayerCvHighlight;
import com.mvpiq.service.PdfGenerationService;
import com.mvpiq.service.PlayerCvService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/api/players/{playerId}/cv")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PlayerCvResource {

    @Inject
    PlayerCvService service;

    @Inject
    PdfGenerationService pdfGenerationService;

    @GET
    public PlayerCvDTO get(@PathParam("playerId") UUID playerId) {
        return service.getCv(playerId);
    }

    @PUT
    public PlayerCvDTO update(@PathParam("playerId") UUID playerId,
                              PlayerCvDTO dto) {
        return service.updateCv(playerId, dto);
    }

    // ===============================
    // SHARING ENDPOINTS
    // ===============================
    @POST
    @Path("/share")
    public Map<String, Object> enableSharing(@PathParam("playerId") UUID playerId) {
        return service.enableSharing(playerId);
    }

    @DELETE
    @Path("/share")
    public Map<String, Object> disableSharing(@PathParam("playerId") UUID playerId) {
        return service.disableSharing(playerId);
    }

    // ===============================
    // HIGHLIGHTS ENDPOINTS
    // ===============================
    @GET
    @Path("/highlights")
    public List<PlayerCvHighlight> getHighlights(@PathParam("playerId") UUID playerId) {
        return service.getHighlights(playerId);
    }

    @POST
    @Path("/highlights")
    public PlayerCvHighlight addHighlight(
            @PathParam("playerId") UUID playerId,
            @FormParam("mediaId") UUID mediaId,
            @FormParam("title") String title,
            @FormParam("description") String description) {
        return service.addHighlight(playerId, mediaId, title, description);
    }

    @POST
    @Path("/highlights/link")
    public PlayerCvHighlight addExternalHighlight(
            @PathParam("playerId") UUID playerId,
            @FormParam("externalUrl") String externalUrl,
            @FormParam("title") String title,
            @FormParam("description") String description) {
        return service.addExternalHighlight(playerId, externalUrl, title, description);
    }

    @DELETE
    @Path("/highlights/{highlightId}")
    public void deleteHighlight(
            @PathParam("playerId") UUID playerId,
            @PathParam("highlightId") UUID highlightId) {
        service.deleteHighlight(playerId, highlightId);
    }

    // ===============================
    // PDF GENERATION ENDPOINTS
    // ===============================
    @GET
    @Path("/pdf")
    @Produces("application/pdf")
    public Response generatePdf(@PathParam("playerId") UUID playerId) {
        byte[] pdf = pdfGenerationService.generatePdf(playerId);
        String filename = "cv_" + playerId.toString() + ".pdf";
        return Response.ok(pdf)
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .build();
    }
}
