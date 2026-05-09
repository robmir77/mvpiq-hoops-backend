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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.OffsetDateTime;
import java.util.ArrayList;
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
                
                // Check if it's a connection error and provide fallback
                if (e.getCause() != null && e.getCause().getMessage() != null) {
                    String causeMessage = e.getCause().getMessage().toLowerCase();
                    if (causeMessage.contains("connection") || causeMessage.contains("network") || 
                        causeMessage.contains("refused") || causeMessage.contains("timeout")) {
                        
                        log.warn("Ollama not available, generating fallback program");
                        
                        // Generate fallback program
                        TrainingProgramDTO fallbackProgram = generateFallbackProgram(request);
                        program.setProgramJson(fallbackProgram);
                        program.setGenerationStatus(GenerationStatus.COMPLETED);
                        program.setGeneratedAt(OffsetDateTime.now());
                        program.setAiModel("FALLBACK");
                        program.setAiPrompt("Fallback program - Ollama unavailable");
                        
                        trainingProgramRepository.persist(program);
                        return program;
                    }
                }
                
                program.setGenerationStatus(GenerationStatus.FAILED);
                throw new RuntimeException("Failed to generate training program: " + e.getMessage(), e);
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
    
    private TrainingProgramDTO generateFallbackProgram(TrainingGenerationRequestDTO request) {
        List<TrainingProgramDTO.TrainingWeekDTO> weeks = new ArrayList<>();
        
        for (int week = 1; week <= request.getWeeks(); week++) {
            List<TrainingProgramDTO.TrainingDayDTO> days = new ArrayList<>();
            
            for (int day = 1; day <= request.getSessionsPerWeek(); day++) {
                List<TrainingProgramDTO.TrainingExerciseDTO> exercises = generateFallbackExercises(request.getSessionDurationMinutes());
                
                days.add(TrainingProgramDTO.TrainingDayDTO.builder()
                        .day(day)
                        .title(String.format("Giorno %d - Sessione di %d minuti", day, request.getSessionDurationMinutes()))
                        .exercises(exercises)
                        .build());
            }
            
            weeks.add(TrainingProgramDTO.TrainingWeekDTO.builder()
                    .week(week)
                    .days(days)
                    .build());
        }
        
        return TrainingProgramDTO.builder()
                .title("Programma di Allenamento - " + request.getGoal())
                .weeks(weeks)
                .build();
    }
    
    private List<TrainingProgramDTO.TrainingExerciseDTO> generateFallbackExercises(Integer sessionDuration) {
        List<TrainingProgramDTO.TrainingExerciseDTO> exercises = new ArrayList<>();
        
        // Warm-up (10 minutes)
        exercises.add(TrainingProgramDTO.TrainingExerciseDTO.builder()
                .exerciseId("warmup")
                .name("Riscaldamento")
                .durationMinutes(10)
                .repetitions(null)
                .notes("Corsa leggera e stretching dinamico")
                .build());
        
        // Main exercises (sessionDuration - 20 minutes for warm-up and cool-down)
        int mainDuration = sessionDuration - 20;
        exercises.add(TrainingProgramDTO.TrainingExerciseDTO.builder()
                .exerciseId("shooting")
                .name("Esercizi di tiro")
                .durationMinutes(mainDuration / 2)
                .repetitions(50)
                .notes("Tiro da diverse posizioni, focus sulla tecnica")
                .build());
        
        exercises.add(TrainingProgramDTO.TrainingExerciseDTO.builder()
                .exerciseId("dribbling")
                .name("Dribbling e handling")
                .durationMinutes(mainDuration / 2)
                .repetitions(100)
                .notes("Esercizi di dribbling con entrambe le mani")
                .build());
        
        // Cool-down (10 minutes)
        exercises.add(TrainingProgramDTO.TrainingExerciseDTO.builder()
                .exerciseId("cooldown")
                .name("Defaticamento")
                .durationMinutes(10)
                .repetitions(null)
                .notes("Stretching statico e recupero")
                .build());
        
        return exercises;
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
