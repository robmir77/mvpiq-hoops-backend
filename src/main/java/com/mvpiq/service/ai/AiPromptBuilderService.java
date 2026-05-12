package com.mvpiq.service.ai;

import com.mvpiq.dto.ai.TrainingGenerationRequestDTO;
import com.mvpiq.model.Player;
import com.mvpiq.model.AthleteGoal;
import com.mvpiq.repositories.ExerciseRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
@Slf4j
public class AiPromptBuilderService {
    
    @Inject
    ExerciseRepository exerciseRepository;
    
    public String buildPrompt(TrainingGenerationRequestDTO request, Player athlete, AthleteGoal goal) {
        // Recupera catalogo esercizi disponibili
        List<String> availableExercises = exerciseRepository.findAll()
                .stream()
                .map(exercise -> exercise.getTitle() + " (ID: " + exercise.getId() + ")")
                .toList();
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("Create a ").append(request.getWeeks()).append("-week basketball training program.\n\n");
        
        // Player Information
        prompt.append("Player Information:\n");
        if (athlete != null) {
            prompt.append("- Age: ").append(athlete.getApproximateAge() != null ? athlete.getApproximateAge() : "N/A").append("\n");
            prompt.append("- Position: ").append(request.getPosition() != null ? request.getPosition() : "N/A").append("\n");
            prompt.append("- Skill Level: ").append(request.getSkillLevel() != null ? request.getSkillLevel() : "INTERMEDIATE").append("\n");
            prompt.append("- Height: ").append(athlete.getHeightCm() != null ? athlete.getHeightCm() + "cm" : "N/A").append("\n");
        }
        
        // Goal
        prompt.append("\nGoal:\n");
        prompt.append("- ").append(request.getGoal() != null ? request.getGoal() : "Improve overall basketball skills").append("\n");
        
        // Availability
        prompt.append("\nAvailability:\n");
        prompt.append("- Sessions per week: ").append(request.getSessionsPerWeek() != null ? request.getSessionsPerWeek() : 3).append("\n");
        prompt.append("- Session duration: ").append(request.getSessionDurationMinutes() != null ? request.getSessionDurationMinutes() : 60).append(" minutes\n");
        
        // Additional notes
        if (request.getAdditionalNotes() != null && !request.getAdditionalNotes().trim().isEmpty()) {
            prompt.append("\nAdditional Requirements:\n");
            prompt.append("- ").append(request.getAdditionalNotes()).append("\n");
        }
        
        // Exercise constraints
        prompt.append("\nExercise Catalog:\n");
        prompt.append("Use ONLY exercises from this catalog. Each exercise must include the exercise ID:\n");
        for (String exercise : availableExercises) {
            prompt.append("- ").append(exercise).append("\n");
        }
        
        // Output format
        prompt.append("\nRequirements:\n");
        prompt.append("- Return valid JSON only\n");
        prompt.append("- Each week should have exactly ").append(request.getSessionsPerWeek()).append(" training days\n");
        prompt.append("- Each session should not exceed ").append(request.getSessionDurationMinutes()).append(" minutes\n");
        prompt.append("- Include proper warm-up and cool-down exercises\n");
        prompt.append("- Progress difficulty appropriately across weeks\n");
        
        // JSON Structure
        prompt.append("\nJSON Structure:\n");
        prompt.append("{\n");
        prompt.append("  \"title\": \"Program Title\",\n");
        prompt.append("  \"weeks\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"week\": 1,\n");
        prompt.append("      \"days\": [\n");
        prompt.append("        {\n");
        prompt.append("          \"day\": 1,\n");
        prompt.append("          \"title\": \"Session Title\",\n");
        prompt.append("          \"exercises\": [\n");
        prompt.append("            {\n");
        prompt.append("              \"exerciseId\": \"uuid-exercise\",\n");
        prompt.append("              \"name\": \"Exercise Name\",\n");
        prompt.append("              \"durationMinutes\": 10,\n");
        prompt.append("              \"repetitions\": 50,\n");
        prompt.append("              \"notes\": \"Technique tips\"\n");
        prompt.append("            }\n");
        prompt.append("          ]\n");
        prompt.append("        }\n");
        prompt.append("      ]\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n");
        
        String finalPrompt = prompt.toString();
        log.debug("Generated AI prompt: {}", finalPrompt);
        
        return finalPrompt;
    }
}
