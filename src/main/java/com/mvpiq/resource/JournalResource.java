package com.mvpiq.resource;

import com.mvpiq.dto.JournalEntryDTO;
import com.mvpiq.service.JournalService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.UUID;

@Path("/api/players/{playerId}/journal")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class JournalResource {

    @Inject
    JournalService service;

    @GET
    public List<JournalEntryDTO> list(@PathParam("playerId") UUID playerId) {
        return service.getPlayerEntries(playerId);
    }

    @GET
    @Path("/{entryId}")
    public JournalEntryDTO get(@PathParam("playerId") UUID playerId,
                               @PathParam("entryId") UUID entryId) {
        return service.getById(playerId, entryId);
    }

    @POST
    public JournalEntryDTO create(@PathParam("playerId") UUID playerId,
                                  JournalEntryDTO dto) {
        return service.create(playerId, dto);
    }

    @DELETE
    @Path("/{entryId}")
    public void delete(@PathParam("playerId") UUID playerId,
                       @PathParam("entryId") UUID entryId) {
        service.delete(playerId, entryId);
    }
}