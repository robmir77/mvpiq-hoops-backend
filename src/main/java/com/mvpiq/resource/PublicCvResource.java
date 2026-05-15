package com.mvpiq.resource;

import com.mvpiq.dto.PlayerCvDTO;
import com.mvpiq.service.PdfGenerationService;
import com.mvpiq.service.PlayerCvService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

@Path("/public/cv")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PublicCvResource {

    @Inject
    PlayerCvService service;

    @Inject
    PdfGenerationService pdfGenerationService;

    @GET
    @Path("/{shareToken}")
    public PlayerCvDTO getPublicCv(@PathParam("shareToken") UUID shareToken) {
        return service.getPublicCv(shareToken);
    }

    @GET
    @Path("/{shareToken}/pdf")
    @Produces("application/pdf")
    public Response generatePublicPdf(@PathParam("shareToken") UUID shareToken) {
        byte[] pdf = pdfGenerationService.generatePublicPdf(shareToken);
        String filename = "cv_public_" + shareToken.toString() + ".pdf";
        return Response.ok(pdf)
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .build();
    }
}
