package com.mvpiq.resource;

import com.mvpiq.dto.PlayerCvDTO;
import com.mvpiq.service.PlayerCvService;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

/**
 * Endpoint pubblici per il CV condiviso.
 * Non richiedono autenticazione — accessibili da chiunque abbia il link.
 *
 * GET /public/cv/{token}       → JSON (usato dall'app)
 * GET /public/cv/{token}/view  → HTML (aperto dal browser / mail)
 */
@Path("/public/cv")
@PermitAll
public class PublicCvResource {

    @Inject
    PlayerCvService service;

    // ─── JSON (per uso da app o integrazioni) ─────────────────
    @GET
    @Path("/{token}")
    @Produces(MediaType.APPLICATION_JSON)
    public PlayerCvDTO getJson(@PathParam("token") UUID token) {
        return service.getPublicCv(token);
    }

    // ─── HTML (aperto nel browser, inviato via mail) ──────────
    @GET
    @Path("/{token}/view")
    @Produces(MediaType.TEXT_HTML + ";charset=UTF-8")
    public Response getHtml(@PathParam("token") UUID token) {
        String html = service.getPublicCvHtml(token);
        return Response.ok(html)
                .header("X-Frame-Options", "SAMEORIGIN")
                .header("Cache-Control", "public, max-age=300")
                .build();
    }
}
