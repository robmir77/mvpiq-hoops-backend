package com.mvpiq.resource;

import com.mvpiq.dto.PlayerCvDTO;
import com.mvpiq.service.PlayerCvService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.UUID;

@Path("/public/cv")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PublicCvResource {

    @Inject
    PlayerCvService service;

    @GET
    @Path("/{shareToken}")
    public PlayerCvDTO getPublicCv(@PathParam("shareToken") UUID shareToken) {
        return service.getPublicCv(shareToken);
    }
}
