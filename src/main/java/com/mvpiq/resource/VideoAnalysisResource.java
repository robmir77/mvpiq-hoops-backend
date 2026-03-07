package com.mvpiq.resource;

import com.mvpiq.dto.AnalysisSessionResponseDTO;
import com.mvpiq.dto.CreateAnalysisSessionRequestDTO;
import com.mvpiq.model.VideoAnalysisType;
import com.mvpiq.repositories.VideoAnalysisTypeRepository;
import com.mvpiq.service.VideoAnalysisService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/api/analysis")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VideoAnalysisResource {

    @Inject
    VideoAnalysisService service;

    @Inject
    VideoAnalysisTypeRepository typeRepository;

    @GET
    @Path("/types")
    public List<VideoAnalysisType> getTypes() {
        return typeRepository.findActive();
    }

    @POST
    @Path("/sessions")
    public AnalysisSessionResponseDTO createSession(CreateAnalysisSessionRequestDTO request) {
        return service.createSession(request);
    }
}