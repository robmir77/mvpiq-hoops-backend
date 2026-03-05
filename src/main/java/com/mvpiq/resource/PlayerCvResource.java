package com.mvpiq.resource;

import com.mvpiq.dto.PlayerCvDTO;
import com.mvpiq.service.PlayerCvService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.UUID;

@Path("/api/players/{playerId}/cv")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PlayerCvResource {

    @Inject
    PlayerCvService service;

    @GET
    public PlayerCvDTO get(@PathParam("playerId") UUID playerId) {
        return service.getCv(playerId);
    }

    @PUT
    public PlayerCvDTO update(@PathParam("playerId") UUID playerId,
                              PlayerCvDTO dto) {
        return service.updateCv(playerId, dto);
    }
}
