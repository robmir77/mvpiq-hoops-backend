package com.mvpiq.resource;

import com.mvpiq.dto.UserDTO;
import com.mvpiq.repositories.UserRepository;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

@Path("/api")
@RequestScoped
public class UserResource {

    @Inject
    UserRepository userRepository;

    // --- Recupera i dati dell'utente corrente ---
    @GET
    @Path("/users/me/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCurrentUser(@PathParam("userId") UUID userId) {
        if (userId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Missing userId parameter").build();
        }

        return userRepository.findByIdOptional(userId)
                .map(UserDTO::fromEntity)
                .map(dto -> Response.ok(dto).build())
                .orElse(Response.status(Response.Status.NOT_FOUND)
                        .entity("User not found").build());
    }
}
