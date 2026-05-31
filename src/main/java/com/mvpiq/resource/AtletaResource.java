package com.mvpiq.resource;

import com.mvpiq.dto.PlayerDTO;
import com.mvpiq.repositories.PlayerRepository;
import com.mvpiq.service.PlayerPositionService;
import com.mvpiq.service.SupabaseStorageService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

@Path("/api")
@RequestScoped
public class AtletaResource {

    @Inject
    PlayerRepository playerRepository;

    @Inject
    PlayerPositionService playerPositionService;

    @Inject
    SupabaseStorageService supabaseStorageService;

    // GET all athlete profiles
    @GET
    @Path("/athletes")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAthleteProfiles() {

        List<PlayerDTO> list = playerRepository.findAll().stream()
                .map(PlayerDTO::fromEntity)
                .toList();

        return Response.ok(list).build();
    }

    // GET single profile by profile ID
    @GET
    @Path("/athlet/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAthleteProfile(@PathParam("id") UUID id) {

        return playerRepository.findByIdOptional(id)
                .map(PlayerDTO::fromEntity)
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
                .map(PlayerDTO::fromEntity)
                .map(Response::ok)
                .orElse(Response.status(Response.Status.NOT_FOUND))
                .build();
    }

    @PUT
    @Path("/athlet/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response updateAthleteProfile(@PathParam("id") UUID id,
                                         PlayerDTO dto) {

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
                            PlayerDTO.fromEntity(player)
                    ).build();
                })
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    // --- Upload profile image ---
    @POST
    @Path("/athlet/{id}/profile-image")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response uploadProfileImageMultipart(
            @PathParam("id") UUID id,
            @org.jboss.resteasy.annotations.providers.multipart.MultipartForm ProfileImageForm form) {

        return playerRepository.findByIdOptional(id)
                .map(player -> {
                    try {
                        if (form == null || form.file == null) {
                            return Response.status(Response.Status.BAD_REQUEST)
                                    .entity("{\"error\": \"No file provided\"}")
                                    .build();
                        }

                        String fileName = form.fileName != null ? form.fileName : "profile.jpg";
                        String ext = getFileExtension(fileName);
                        String ct = getContentType(fileName);

                        java.nio.file.Path tempPath = Files.createTempFile("profile-image-", ext);
                        File tempFile = tempPath.toFile();
                        Files.copy(form.file, tempPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                        String timestamp = String.valueOf(System.currentTimeMillis());
                        String path = player.getUsername() + "/profile-" + timestamp + ext;

                        String imageUrl = supabaseStorageService.uploadProfileImage(tempFile, path, ct);

                        player.setAvatarUrl(imageUrl);
                        playerRepository.persist(player);

                        Files.deleteIfExists(tempPath);

                        return Response.ok("{\"avatarUrl\": \"" + imageUrl + "\"}").build();

                    } catch (IOException e) {
                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                .entity("{\"error\": \"Failed to process image: " + e.getMessage() + "\"}")
                                .build();
                    } catch (Exception e) {
                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                .entity("{\"error\": \"Upload failed: " + e.getMessage() + "\"}")
                                .build();
                    }
                })
                .orElse(Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\": \"Player not found\"}")
                        .build());
    }

    // Endpoint JSON fallback: aggiorna solo l'URL già caricato altrove
    @POST
    @Path("/athlet/{id}/profile-image")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response uploadProfileImageJson(
            @PathParam("id") UUID id,
            java.util.Map<String, Object> body) {

        return playerRepository.findByIdOptional(id)
                .map(player -> {
                    String avatarUrl = body != null ? (String) body.get("avatarUrl") : null;
                    if (avatarUrl == null) {
                        return Response.status(Response.Status.BAD_REQUEST)
                                .entity("{\"error\": \"avatarUrl is required\"}")
                                .build();
                    }

                    player.setAvatarUrl(avatarUrl);
                    playerRepository.persist(player);
                    return Response.ok("{\"avatarUrl\": \"" + avatarUrl + "\"}").build();
                })
                .orElse(Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\": \"Player not found\"}")
                        .build());
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf("."));
    }

    private String getContentType(String filename) {
        if (filename == null) return "image/jpeg";
        switch (getFileExtension(filename).toLowerCase()) {
            case ".png":  return "image/png";
            case ".gif":  return "image/gif";
            case ".webp": return "image/webp";
            default:      return "image/jpeg";
        }
    }

    public static class ProfileImageForm {
        @org.jboss.resteasy.annotations.jaxrs.FormParam("file")
        @org.jboss.resteasy.annotations.providers.multipart.PartType("application/octet-stream")
        public java.io.InputStream file;

        @org.jboss.resteasy.annotations.jaxrs.FormParam("fileName")
        @org.jboss.resteasy.annotations.providers.multipart.PartType("text/plain")
        public String fileName;
    }
}
