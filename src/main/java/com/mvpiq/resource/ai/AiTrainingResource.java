package com.mvpiq.resource.ai;

import com.mvpiq.dto.ApiResponse;
import com.mvpiq.dto.ai.TrainingGenerationRequestDTO;
import com.mvpiq.model.TrainingProgram;
import com.mvpiq.service.ai.AiTrainingService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

@Path("/api/ai/training-programs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Slf4j
public class AiTrainingResource {
    
    @Inject
    AiTrainingService aiTrainingService;
    
    @POST
    @Path("/generate")
    public Response generateTrainingProgram(TrainingGenerationRequestDTO request) {
        try {
            log.info("Generating training program for athlete: {}", request.getAthleteId());
            
            TrainingProgram program = aiTrainingService.generateTrainingProgram(request);
            
            return Response.status(Response.Status.CREATED)
                    .entity(ApiResponse.success(program, "Training program generation started"))
                    .build();
                    
        } catch (Exception e) {
            log.error("Error generating training program", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Failed to generate training program: " + e.getMessage()))
                    .build();
        }
    }
    
    @GET
    @Path("/{athleteId}")
    public Response getAthleteTrainingPrograms(@PathParam("athleteId") UUID athleteId) {
        try {
            // This would be implemented in TrainingProgramRepository
            List<TrainingProgram> programs = aiTrainingService.getAthletePrograms(athleteId);
            
            return Response.ok(ApiResponse.success(programs, "Training programs retrieved successfully"))
                    .build();
                    
        } catch (Exception e) {
            log.error("Error retrieving training programs for athlete: {}", athleteId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Failed to retrieve training programs: " + e.getMessage()))
                    .build();
        }
    }
    
    @GET
    @Path("/program/{programId}")
    public Response getTrainingProgram(@PathParam("programId") UUID programId) {
        try {
            TrainingProgram program = aiTrainingService.getProgramById(programId);
            
            if (program == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(ApiResponse.error("Training program not found"))
                        .build();
            }
            
            return Response.ok(ApiResponse.success(program, "Training program retrieved successfully"))
                    .build();
                    
        } catch (Exception e) {
            log.error("Error retrieving training program: {}", programId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Failed to retrieve training program: " + e.getMessage()))
                    .build();
        }
    }
    
    @POST
    @Path("/{programId}/regenerate")
    public Response regenerateTrainingProgram(@PathParam("programId") UUID programId) {
        try {
            log.info("Regenerating training program: {}", programId);
            
            TrainingProgram regeneratedProgram = aiTrainingService.regenerateProgram(programId);
            
            return Response.status(Response.Status.CREATED)
                    .entity(ApiResponse.success(regeneratedProgram, "Training program regenerated successfully"))
                    .build();
                    
        } catch (Exception e) {
            log.error("Error regenerating training program: {}", programId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Failed to regenerate training program: " + e.getMessage()))
                    .build();
        }
    }
    
    @GET
    @Path("/status/{programId}")
    public Response getGenerationStatus(@PathParam("programId") UUID programId) {
        try {
            TrainingProgram program = aiTrainingService.getProgramById(programId);
            
            if (program == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(ApiResponse.error("Training program not found"))
                        .build();
            }
            
            return Response.ok(ApiResponse.success(
                    new GenerationStatusDTO(program.getGenerationStatus(), program.getGeneratedAt()),
                    "Generation status retrieved successfully"))
                    .build();
                    
        } catch (Exception e) {
            log.error("Error retrieving generation status for program: {}", programId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Failed to retrieve generation status: " + e.getMessage()))
                    .build();
        }
    }
    
    // DTO for generation status response
    @Setter
    @Getter
    public static class GenerationStatusDTO {
        // Getters and setters
        private String status;
        private java.time.OffsetDateTime generatedAt;
        
        public GenerationStatusDTO(com.mvpiq.enums.GenerationStatus status, java.time.OffsetDateTime generatedAt) {
            this.status = status.name();
            this.generatedAt = generatedAt;
        }

    }
}
