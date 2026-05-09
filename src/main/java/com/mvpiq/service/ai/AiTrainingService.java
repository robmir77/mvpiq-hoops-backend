package com.mvpiq.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mvpiq.client.OllamaClient;
import com.mvpiq.dto.ai.OllamaRequestDTO;
import com.mvpiq.dto.ai.OllamaResponseDTO;
import com.mvpiq.dto.ai.TrainingGenerationRequestDTO;
import com.mvpiq.dto.ai.TrainingProgramDTO;
import com.mvpiq.enums.GenerationStatus;
import com.mvpiq.enums.SourceType;
import com.mvpiq.model.AthleteGoal;
import com.mvpiq.model.PlayerProfile;
import com.mvpiq.model.TrainingProgram;
import com.mvpiq.repositories.AthleteGoalsRepository;
import com.mvpiq.repositories.PlayerRepository;
import com.mvpiq.repositories.TrainingProgramRepository;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
@Slf4j
public class AiTrainingService {
    
    @Inject
    @RestClient
    OllamaClient ollamaClient;
    
    @Inject
    AiPromptBuilderService promptBuilderService;
    
    @Inject
    PlayerRepository playerRepository;
    
    @Inject
    AthleteGoalsRepository athleteGoalsRepository;
    
    @Inject
    TrainingProgramRepository trainingProgramRepository;
    
    @Inject
    ObjectMapper objectMapper;
    
    @Transactional
    public TrainingProgram generateTrainingProgram(TrainingGenerationRequestDTO request) {
        try {
            // Retrieve athlete profile
            PlayerProfile athlete = playerRepository.findByIdOptional(request.getAthleteId())
                    .orElseThrow(() -> new RuntimeException("Athlete not found"));
            
            // Retrieve goal if specified
            AthleteGoal goal = null;
            if (request.getGoal() != null && !request.getGoal().trim().isEmpty()) {
                goal = athleteGoalsRepository.findByPlayerId(request.getAthleteId())
                        .stream()
                        .filter(g -> g.getTitle().equalsIgnoreCase(request.getGoal()))
                        .findFirst()
                        .orElse(null);
            }
            
            // Build prompt
            String prompt = promptBuilderService.buildPrompt(request, athlete, goal);
            
            // Create training program record with PENDING status
            TrainingProgram program = TrainingProgram.builder()
                    .owner(athlete)
                    .title("AI Generated: " + (request.getGoal() != null ? request.getGoal() : "Custom Training"))
                    .description("AI-generated training program")
                    .sourceType(SourceType.AI_GENERATED)
                    .goal(goal)
                    .generatedByAi(true)
                    .aiModel("qwen3:8b")
                    .aiPrompt(prompt)
                    .aiGenerationParameters(request)
                    .generationStatus(GenerationStatus.PENDING)
                    .isPublic(false)
                    .build();
            
            trainingProgramRepository.persist(program);
            
            try {
                // Update status to GENERATING
                program.setGenerationStatus(GenerationStatus.GENERATING);
                trainingProgramRepository.persist(program);
                
                // Call Ollama API
                OllamaRequestDTO ollamaRequest = OllamaRequestDTO.builder()
                        .model("qwen3:8b")
                        .prompt(prompt)
                        .options(OllamaRequestDTO.OllamaOptionsDTO.builder()
                                .temperature(0.7)
                                .top_p(90)
                                .max_tokens(2000)
                                .build())
                        .build();
                
                OllamaResponseDTO response = ollamaClient.generate(ollamaRequest);
                
                if (response != null && response.getResponse() != null) {
                    // Parse AI response
                    TrainingProgramDTO aiProgram = parseAiResponse(response.getResponse());
                    
                    // Update program with AI-generated content
                    program.setProgramJson(aiProgram);
                    program.setGenerationStatus(GenerationStatus.COMPLETED);
                    program.setGeneratedAt(OffsetDateTime.now());
                } else {
                    program.setGenerationStatus(GenerationStatus.FAILED);
                }
                
            } catch (Exception e) {
                log.error("Error generating AI training program", e);
                program.setGenerationStatus(GenerationStatus.FAILED);
            }
            
            trainingProgramRepository.persist(program);
            return program;
            
        } catch (Exception e) {
            log.error("Error in generateTrainingProgram", e);
            throw new RuntimeException("Failed to generate training program", e);
        }
    }
    
    private TrainingProgramDTO parseAiResponse(String aiResponse) {
        try {
            // Extract JSON from response (in case there's extra text)
            String jsonContent = extractJsonFromResponse(aiResponse);
            return objectMapper.readValue(jsonContent, TrainingProgramDTO.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse AI response as JSON: {}", aiResponse, e);
            throw new RuntimeException("Invalid AI response format", e);
        }
    }
    
    private String extractJsonFromResponse(String response) {
        // Look for JSON object boundaries
        int start = response.indexOf("{");
        int end = response.lastIndexOf("}");
        
        if (start != -1 && end != -1 && end > start) {
            return response.substring(start, end + 1);
        }
        
        throw new RuntimeException("No valid JSON found in AI response");
    }
    
    public List<TrainingProgram> getAthletePrograms(UUID athleteId) {
        return trainingProgramRepository.findByOwnerId(athleteId);
    }
    
    public TrainingProgram getProgramById(UUID programId) {
        return trainingProgramRepository.findById(programId);
    }
    
    @Transactional
    public TrainingProgram regenerateProgram(UUID programId) {
        TrainingProgram existingProgram = trainingProgramRepository.findByIdOptional(programId)
                .orElseThrow(() -> new RuntimeException("Program not found"));
        
        if (existingProgram.getAiPrompt() == null) {
            throw new RuntimeException("Cannot regenerate non-AI program");
        }
        
        // Create new program as adaptation of existing one
        TrainingProgram newProgram = TrainingProgram.builder()
                .owner(existingProgram.getOwner())
                .title("Regenerated: " + existingProgram.getTitle())
                .description("Regenerated AI training program")
                .sourceType(SourceType.AI_ADAPTED)
                .goal(existingProgram.getGoal())
                .generatedByAi(true)
                .aiModel(existingProgram.getAiModel())
                .aiPrompt(existingProgram.getAiPrompt())
                .aiGenerationParameters(existingProgram.getAiGenerationParameters())
                .parentProgram(existingProgram)
                .generationStatus(GenerationStatus.PENDING)
                .isPublic(false)
                .build();
        
        trainingProgramRepository.persist(newProgram);
        
        try {
            newProgram.setGenerationStatus(GenerationStatus.GENERATING);
            trainingProgramRepository.persist(newProgram);
            
            // Regenerate with same prompt
            OllamaRequestDTO ollamaRequest = OllamaRequestDTO.builder()
                    .model(newProgram.getAiModel())
                    .prompt(newProgram.getAiPrompt())
                    .options(OllamaRequestDTO.OllamaOptionsDTO.builder()
                            .temperature(0.8) // Slightly higher temperature for regeneration
                            .top_p(90)
                            .max_tokens(2000)
                            .build())
                    .build();
            
            OllamaResponseDTO response = ollamaClient.generate(ollamaRequest);
            
            if (response != null && response.getResponse() != null) {
                TrainingProgramDTO aiProgram = parseAiResponse(response.getResponse());
                newProgram.setProgramJson(aiProgram);
                newProgram.setGenerationStatus(GenerationStatus.COMPLETED);
                newProgram.setGeneratedAt(OffsetDateTime.now());
            } else {
                newProgram.setGenerationStatus(GenerationStatus.FAILED);
            }
            
        } catch (Exception e) {
            log.error("Error regenerating training program", e);
            newProgram.setGenerationStatus(GenerationStatus.FAILED);
        }
        
        trainingProgramRepository.persist(newProgram);
        return newProgram;
    }
}
