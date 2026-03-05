package com.mvpiq.resource;

import com.mvpiq.dto.LoginDTO;
import com.mvpiq.dto.LoginResponseDTO;
import com.mvpiq.dto.RegisterDTO;
import com.mvpiq.model.User;
import com.mvpiq.service.AuthService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api")
@RequestScoped
public class AuthResource {

    @Inject
    AuthService authService;

    @POST
    @Path("/auth/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response register(RegisterDTO dto) {
        User user = authService.register(dto);
        return Response.ok(user).build();
    }

    @POST
    @Path("/auth/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(LoginDTO dto) {

        LoginResponseDTO response = authService.login(dto);

        return Response.ok(response).build();
    }
}
