package com.mvpiq.client;

import com.mvpiq.dto.ai.OllamaRequestDTO;
import com.mvpiq.dto.ai.OllamaResponseDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/api/generate")
@RegisterRestClient(configKey = "ollama-api")
@ApplicationScoped
@RegisterClientHeaders
public interface OllamaClient {
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    OllamaResponseDTO generate(OllamaRequestDTO request);
}
