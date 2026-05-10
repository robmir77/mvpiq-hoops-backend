package com.mvpiq.resource;

import com.mvpiq.model.ChecklistTemplate;
import com.mvpiq.model.ChecklistTemplateItemOption;
import com.mvpiq.repositories.ChecklistTemplateRepository;
import com.mvpiq.security.RoleBasedSecurityService;
import com.mvpiq.service.ChecklistDynamicOptionsService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Path("/api/checklist-templates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Slf4j
public class ChecklistTemplateResource {

    @Inject
    ChecklistTemplateRepository repository;

    @Inject
    RoleBasedSecurityService securityService;

    @Inject
    ChecklistDynamicOptionsService dynamicOptionsService;

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

    /**
     * GET /api/checklist-templates/all
     * Recupera tutti i template (solo admin)
     */
    @GET
    @Path("/all")
    public Response getAll() {
        if (!securityService.canManageUsers()) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        return Response.ok(repository.findAllActive()).build();
    }

    /**
     * GET /api/checklist-templates/{id}
     * Recupera un template specifico (solo admin)
     */
    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") UUID id) {
        if (!securityService.canManageUsers()) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        ChecklistTemplate template = repository.findById(id);
        if (template == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(template).build();
    }

    /**
     * POST /api/checklist-templates
     * Crea un nuovo template (solo admin)
     */
    @POST
    public Response create(ChecklistTemplate template) {
        if (!securityService.canManageUsers()) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        if (template.getCode() == null || template.getCode().isBlank()) {
            throw new BadRequestException("code is required");
        }
        if (template.getName() == null || template.getName().isBlank()) {
            throw new BadRequestException("name is required");
        }
        if (template.getEntryType() == null || template.getEntryType().isBlank()) {
            throw new BadRequestException("entryType is required");
        }

        template.setCreatedAt(OffsetDateTime.now());
        template.setIsActive(true);

        repository.persist(template);
        return Response.status(Response.Status.CREATED).entity(template).build();
    }

    /**
     * PUT /api/checklist-templates/{id}
     * Aggiorna un template esistente (solo admin)
     */
    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") UUID id, ChecklistTemplate template) {
        if (!securityService.canManageUsers()) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        ChecklistTemplate existing = repository.findById(id);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        if (template.getName() != null) {
            existing.setName(template.getName());
        }
        if (template.getEntryType() != null) {
            existing.setEntryType(template.getEntryType());
        }
        if (template.getIsActive() != null) {
            existing.setIsActive(template.getIsActive());
        }
        if (template.getItems() != null) {
            existing.setItems(template.getItems());
        }

        repository.persist(existing);
        return Response.ok(existing).build();
    }

    /**
     * DELETE /api/checklist-templates/{id}
     * Cancella un template (solo admin)
     */
    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") UUID id) {
        if (!securityService.canManageUsers()) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        boolean deleted = repository.deleteById(id);
        if (!deleted) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }

    /**
     * GET /api/checklist-templates/dynamic-options?selectSource=POSITION_METADATA
     * Recupera le opzioni dinamiche basate su select_source per SELECT/MULTI_SELECT
     */
    @GET
    @Path("/dynamic-options")
    public Response getDynamicOptions(
            @QueryParam("selectSource") String selectSource,
            @QueryParam("selectQuery") String selectQuery
    ) {
        log.info("Dynamic options requested - selectSource: {}, selectQuery: {}", selectSource, selectQuery);
        
        if (selectSource == null || selectSource.isBlank()) {
            log.warn("selectSource is null or blank");
            throw new BadRequestException("selectSource is required");
        }

        if ("SQL".equals(selectSource) && (selectQuery == null || selectQuery.isBlank())) {
            log.warn("selectSource is SQL but selectQuery is null or blank");
            throw new BadRequestException("selectQuery is required when selectSource is SQL");
        }

        List<ChecklistTemplateItemOption> options = dynamicOptionsService.getDynamicOptions(selectSource, selectQuery);
        log.info("Returning {} dynamic options for selectSource: {}", options.size(), selectSource);
        return Response.ok(options).build();
    }
}