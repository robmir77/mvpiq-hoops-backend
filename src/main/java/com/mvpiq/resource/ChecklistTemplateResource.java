package com.mvpiq.resource;

import com.mvpiq.model.ChecklistTemplate;
import com.mvpiq.repositories.ChecklistTemplateRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/api/checklist-templates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ChecklistTemplateResource {

    @Inject
    ChecklistTemplateRepository repository;

    /**
     * GET /api/checklist-templates?entryType=MATCH
     */
    @GET
    public List<ChecklistTemplate> getByEntryType(
            @QueryParam("entryType") String entryType
    ) {

        if (entryType == null || entryType.isBlank()) {
            throw new BadRequestException("entryType is required");
        }

        return repository.findActiveByType(entryType);
    }
}