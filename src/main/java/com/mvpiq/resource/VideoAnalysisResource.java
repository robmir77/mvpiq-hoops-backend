package com.mvpiq.resource;

import com.mvpiq.dto.AnalysisResultDTO;
import com.mvpiq.dto.AnalysisSessionResponseDTO;
import com.mvpiq.dto.CreateAnalysisSessionRequestDTO;
import com.mvpiq.dto.VideoAnalysisRequestDTO;
import com.mvpiq.model.VideoAnalysisType;
import com.mvpiq.repositories.VideoAnalysisTypeRepository;
import com.mvpiq.service.VideoAnalysisService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.UUID;

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

    @POST
    @Path("/analyze")
    public AnalysisResultDTO analyzeVideo(VideoAnalysisRequestDTO request) {
        return service.analyzeVideo(request);
    }

    @GET
    @Path("/sessions/{id}/result")
    public AnalysisResultDTO getResult(@PathParam("id") UUID sessionId) {
        return service.getResult(sessionId);
    }
}