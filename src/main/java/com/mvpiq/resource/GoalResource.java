package com.mvpiq.resource;

import com.mvpiq.dto.AthleteGoalDTO;
import com.mvpiq.model.AthleteGoal;
import com.mvpiq.repositories.AthleteGoalsRepository;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/api")
@RequestScoped
public class GoalResource {

    @Inject
    AthleteGoalsRepository athleteGoalsRepository;

    // --- GET athlete goals ---
    @GET
    @Path("/goals/{athleteId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAthleteGoals(@PathParam("athleteId") UUID athleteId) {
        List<AthleteGoalDTO> goals = athleteGoalsRepository.findByAthleteId(athleteId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return Response.ok(goals).build();
    }

    // --- CREATE athlete goal ---
    @POST
    @Path("/goals/{athleteId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response createAthleteGoal(@PathParam("athleteId") UUID athleteId, AthleteGoalDTO dto) {
        AthleteGoal goal = AthleteGoal.builder()
                .athleteId(athleteId)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .currentValue(dto.getCurrentValue())
                .targetValue(dto.getTargetValue())
                .unit(dto.getUnit())
                .status(dto.getStatus() != null ? dto.getStatus() : "ACTIVE")
                .createdAt(OffsetDateTime.now())
                .build();
        athleteGoalsRepository.save(goal);
        return Response.ok(toDTO(goal)).build();
    }

    // --- UPDATE athlete goal ---
    @PUT
    @Path("/goals/{goalId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response updateAthleteGoal(@PathParam("goalId") UUID goalId, AthleteGoalDTO dto) {
        AthleteGoal goal = athleteGoalsRepository.findById(goalId);
        if (goal == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        goal.setTitle(dto.getTitle());
        goal.setDescription(dto.getDescription());
        goal.setTargetValue(dto.getTargetValue());
        goal.setCurrentValue(dto.getCurrentValue());
        goal.setUnit(dto.getUnit());
        goal.setStatus(dto.getStatus());
        goal.setDueDate(dto.getDueDate());
        goal.setCompletedAt(dto.getCompletedAt());

        athleteGoalsRepository.save(goal); // persist o update
        return Response.ok(toDTO(goal)).build();
    }

    private AthleteGoalDTO toDTO(AthleteGoal goal) {
        return AthleteGoalDTO.builder()
                .id(goal.getId())
                .athleteId(goal.getAthleteId())
                .title(goal.getTitle())
                .description(goal.getDescription())
                .targetValue(goal.getTargetValue())
                .currentValue(goal.getCurrentValue())
                .unit(goal.getUnit())
                .status(goal.getStatus())
                .dueDate(goal.getDueDate())
                .completedAt(goal.getCompletedAt())
                .build();
    }
}
