package com.mvpiq.resource;

import com.mvpiq.dto.PlayerDTO;
import com.mvpiq.model.Player;
import com.mvpiq.repositories.PlayerRepository;
import com.mvpiq.service.SupabaseStorageService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

@Path("/api")
@RequestScoped
public class PlayerResource {

    @Inject
    PlayerRepository playerRepository;

    @Inject
    SupabaseStorageService supabaseStorageService;

    // --- Upload profile image ---
    @PUT
    @Path("/players/{playerId}/profile-image")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response uploadProfileImage(
            @PathParam("playerId") UUID playerId,
            @MultipartForm ProfileImageUploadForm form) {

        Player player = playerRepository.findById(playerId);
        if (player == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Player not found\"}")
                    .build();
        }

        try {
            if (form.getFileInputStream() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\": \"No file provided\"}")
                        .build();
            }

            // Create temporary file
            String fileName = form.getFileName();
            java.nio.file.Path tempPath = Files.createTempFile("profile-image-", getFileExtension(fileName));
            File tempFile = tempPath.toFile();
            
            // Copy input stream to temp file
            Files.copy(form.getFileInputStream(), tempPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            // Determine content type
            String contentType = getContentType(fileName);

            // Create path: {username}/profile.{ext}
            String path = player.getUsername() + "/profile" + getFileExtension(fileName);

            // Upload to Supabase
            String imageUrl = supabaseStorageService.uploadProfileImage(tempFile, path, contentType);

            // Update player avatarUrl
            player.setAvatarUrl(imageUrl);
            playerRepository.persist(player);

            // Clean up temp file
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
    }

    // --- Get player profile ---
    @GET
    @Path("/players/{playerId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPlayer(@PathParam("playerId") UUID playerId) {
        Player player = playerRepository.findById(playerId);
        if (player == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(PlayerDTO.fromEntity(player)).build();
    }

    // --- Update player profile ---
    @PUT
    @Path("/players/{playerId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response updatePlayer(@PathParam("playerId") UUID playerId, PlayerDTO dto) {
        Player player = playerRepository.findById(playerId);
        if (player == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        dto.updateEntity(player);
        playerRepository.persist(player);

        return Response.ok(PlayerDTO.fromEntity(player)).build();
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    private String getContentType(String filename) {
        if (filename == null) {
            return "image/jpeg";
        }
        String extension = getFileExtension(filename).toLowerCase();
        switch (extension) {
            case ".png":
                return "image/png";
            case ".gif":
                return "image/gif";
            case ".webp":
                return "image/webp";
            default:
                return "image/jpeg";
        }
    }

    public static class ProfileImageUploadForm {

        public File file;

        public java.io.InputStream getFileInputStream() throws IOException {
            if (file != null) {
                return new java.io.FileInputStream(file);
            }
            return null;
        }

        public String getFileName() {
            return file != null ? file.getName() : null;
        }
    }
}
