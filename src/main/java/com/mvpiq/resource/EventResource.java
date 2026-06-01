package com.mvpiq.resource;

import com.mvpiq.dto.EventDTO;
import com.mvpiq.dto.EventLocationDTO;
import com.mvpiq.dto.EventParticipantDTO;
import com.mvpiq.service.EventService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

/**
 * REST API per la gestione degli eventi.
 *
 * Endpoints:
 *
 *   EVENTI
 *   GET    /api/events                          → lista eventi pubblici prossimi (filter: ?type=)
 *   GET    /api/events/{eventId}                → dettaglio evento con partecipanti
 *   POST   /api/events                          → crea evento (body: EventDTO, ?creatorId=)
 *   PUT    /api/events/{eventId}                → aggiorna evento (solo creatore)
 *   DELETE /api/events/{eventId}                → soft-delete evento (solo creatore)
 *   GET    /api/events/user/{userId}            → eventi creati da un utente
 *   GET    /api/events/participant/{userId}     → eventi a cui un utente partecipa
 *
 *   PARTECIPAZIONI / RSVP
 *   POST   /api/events/{eventId}/join           → auto-iscrizione (?userId=)
 *   DELETE /api/events/{eventId}/leave          → abbandona evento (?userId=)
 *   POST   /api/events/{eventId}/invite         → invita utente (solo creatore, body: uuid)
 *   PUT    /api/events/{eventId}/rsvp           → aggiorna RSVP (?userId=&status=)
 *   GET    /api/events/{eventId}/participants   → lista partecipanti (filter: ?rsvpStatus=)
 *
 *   LOCATION
 *   GET    /api/event-locations                 → lista location (filter: ?city=)
 *   POST   /api/event-locations                 → crea location (?userId=)
 */
@Path("/api")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class EventResource {

    @Inject
    EventService service;

    // ─────────────────────────────────────────────────────────────────────────
    // EVENTI
    // ─────────────────────────────────────────────────────────────────────────

    @GET
    @Path("/events")
    public List<EventDTO> listUpcoming(@QueryParam("type") String type) {
        return service.getUpcomingEvents(type);
    }

    @GET
    @Path("/events/{eventId}")
    public EventDTO get(@PathParam("eventId") UUID eventId) {
        return service.getById(eventId);
    }

    @GET
    @Path("/events/user/{userId}")
    public List<EventDTO> getByCreator(@PathParam("userId") UUID userId) {
        return service.getByCreator(userId);
    }

    @GET
    @Path("/events/participant/{userId}")
    public List<EventDTO> getByParticipant(@PathParam("userId") UUID userId) {
        return service.getByParticipant(userId);
    }

    @POST
    @Path("/events")
    public Response create(@QueryParam("creatorId") UUID creatorId, EventDTO dto) {
        EventDTO created = service.create(creatorId, dto);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/events/{eventId}")
    public EventDTO update(@QueryParam("creatorId") UUID creatorId,
                           @PathParam("eventId") UUID eventId,
                           EventDTO dto) {
        return service.update(creatorId, eventId, dto);
    }

    @DELETE
    @Path("/events/{eventId}")
    public Response delete(@QueryParam("creatorId") UUID creatorId,
                           @PathParam("eventId") UUID eventId) {
        service.delete(creatorId, eventId);
        return Response.noContent().build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PARTECIPAZIONI / RSVP
    // ─────────────────────────────────────────────────────────────────────────

    @POST
    @Path("/events/{eventId}/join")
    public Response join(@PathParam("eventId") UUID eventId,
                         @QueryParam("userId") UUID userId) {
        EventParticipantDTO participant = service.join(userId, eventId);
        return Response.status(Response.Status.CREATED).entity(participant).build();
    }

    @DELETE
    @Path("/events/{eventId}/leave")
    public Response leave(@PathParam("eventId") UUID eventId,
                          @QueryParam("userId") UUID userId) {
        service.leave(userId, eventId);
        return Response.noContent().build();
    }

    @POST
    @Path("/events/{eventId}/invite")
    public Response invite(@PathParam("eventId") UUID eventId,
                           @QueryParam("creatorId") UUID creatorId,
                           UUID targetUserId) {
        EventParticipantDTO participant = service.invite(creatorId, eventId, targetUserId);
        return Response.status(Response.Status.CREATED).entity(participant).build();
    }

    @PUT
    @Path("/events/{eventId}/rsvp")
    public EventParticipantDTO updateRsvp(@PathParam("eventId") UUID eventId,
                                          @QueryParam("userId") UUID userId,
                                          @QueryParam("status") String status) {
        return service.updateRsvp(userId, eventId, status);
    }

    @GET
    @Path("/events/{eventId}/participants")
    public List<EventParticipantDTO> getParticipants(@PathParam("eventId") UUID eventId,
                                                     @QueryParam("rsvpStatus") String rsvpStatus) {
        return service.getParticipants(eventId, rsvpStatus);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOCATION
    // ─────────────────────────────────────────────────────────────────────────

    @GET
    @Path("/event-locations")
    public List<EventLocationDTO> getLocations(@QueryParam("city") String city) {
        return service.getLocations(city);
    }

    @POST
    @Path("/event-locations")
    public Response createLocation(@QueryParam("userId") UUID userId,
                                   EventLocationDTO dto) {
        EventLocationDTO created = service.createLocation(userId, dto);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }
}
