package com.mvpiq.resource;

import com.mvpiq.dto.MediaAssetDTO;
import com.mvpiq.dto.VideoUploadFormDTO;
import com.mvpiq.service.MediaService;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

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

        List<MediaAssetDTO> videos =
                mediaService.getVideosByAthlete(athleteId);

        return Response.ok(videos).build();
    }

    @POST
    @Path("/videos/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadVideo(@MultipartForm VideoUploadFormDTO form) {

        System.out.println("🎥 UPLOAD ENDPOINT HIT");

        MediaAssetDTO saved =
                mediaService.uploadVideo(form.file, UUID.fromString(form.userId));

        return Response.ok(saved).build();
    }
}