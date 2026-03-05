package com.mvpiq.resource;

import com.mvpiq.dto.PlayerProfileDTO;
import com.mvpiq.repositories.PlayerRepository;
import com.mvpiq.service.PlayerPositionService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/api")
@RequestScoped
public class AtletaResource {

    @Inject
    PlayerRepository playerRepository;

    @Inject
    PlayerPositionService playerPositionService;

    // GET all athlete profiles
    @GET
    @Path("/athletes")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAthleteProfiles() {

        List<PlayerProfileDTO> list = playerRepository.findAll().stream()
                .map(PlayerProfileDTO::fromEntity)
                .toList();

        return Response.ok(list).build();
    }

    // GET single profile by profile ID
    @GET
    @Path("/athlet/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAthleteProfile(@PathParam("id") UUID id) {

        return playerRepository.findByIdOptional(id)
                .map(PlayerProfileDTO::fromEntity)
                .map(Response::ok)
                .orElse(Response.status(Response.Status.NOT_FOUND))
                .build();
    }

    // GET single profile by user ID
    @GET
    @Path("/athlet/user/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAthleteProfileByUserId(@PathParam("userId") UUID userId) {

        return playerRepository.findByIdOptional(userId)
                .map(PlayerProfileDTO::fromEntity)
                .map(Response::ok)
                .orElse(Response.status(Response.Status.NOT_FOUND))
                .build();
    }

    @PUT
    @Path("/athlet/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response updateAthleteProfile(@PathParam("id") UUID id,
                                         PlayerProfileDTO dto) {

        return playerRepository.findByIdOptional(id)
                .map(player -> {

                    // 🔹 Aggiorna campi base
                    dto.updateEntity(player);

                    // 🔥 Aggiorna posizioni (UUID-based)
                    playerPositionService.updatePositions(
                            player,
                            dto.getMainPositionId(),
                            dto.getSecondaryPositionIds()
                    );

                    return Response.ok(
                            PlayerProfileDTO.fromEntity(player)
                    ).build();
                })
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }
}
