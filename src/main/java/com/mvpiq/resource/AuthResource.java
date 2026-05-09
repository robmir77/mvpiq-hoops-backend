package com.mvpiq.resource;

import com.mvpiq.dto.ApiResponse;
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
        LoginResponseDTO response = authService.register(dto);
        ApiResponse<LoginResponseDTO> apiResponse = ApiResponse.success(response, "User registered successfully");
        return Response.ok(apiResponse).build();
    }

    @POST
    @Path("/auth/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(LoginDTO dto) {

        LoginResponseDTO response = authService.login(dto);
        ApiResponse<LoginResponseDTO> apiResponse = ApiResponse.success(response, "Login successful");
        return Response.ok(apiResponse).build();
    }

    @POST
    @Path("/auth/logout")
    @Produces(MediaType.APPLICATION_JSON)
    public Response logout() {
        authService.logout();
        ApiResponse<Void> apiResponse = ApiResponse.success(null, "Logout successful");
        return Response.ok(apiResponse).build();
    }
}
