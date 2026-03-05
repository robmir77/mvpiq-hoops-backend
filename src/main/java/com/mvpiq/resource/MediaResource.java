package com.mvpiq.resource;

import com.mvpiq.dto.MediaAssetDTO;
import com.mvpiq.dto.MediaUploadRequest;
import com.mvpiq.service.MediaService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/api")
@RequestScoped
public class MediaResource {

    @Inject
    MediaService mediaService;

    @GET
    @Path("/videos/{athleteId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVideos(@PathParam("athleteId") UUID athleteId) {

        List<MediaAssetDTO> videos = mediaService.getVideosByAthlete(athleteId);
        return Response.ok(videos).build();
    }

    @POST
    @Path("/videos")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadVideo(MediaUploadRequest request) {

        MediaAssetDTO saved = mediaService.upload(request);
        return Response.ok(saved).build();
    }
}
