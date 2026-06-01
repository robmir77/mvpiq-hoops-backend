package com.mvpiq.service;

import com.mvpiq.dto.EventDTO;
import com.mvpiq.dto.EventLocationDTO;
import com.mvpiq.dto.EventParticipantDTO;
import com.mvpiq.model.Event;
import com.mvpiq.model.EventLocation;
import com.mvpiq.model.EventParticipant;
import com.mvpiq.model.User;
import com.mvpiq.repositories.EventLocationRepository;
import com.mvpiq.repositories.EventParticipantRepository;
import com.mvpiq.repositories.EventRepository;
import com.mvpiq.repositories.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
@Transactional
public class EventService {

    @Inject
    EventRepository eventRepository;

    @Inject
    EventParticipantRepository participantRepository;

    @Inject
    EventLocationRepository locationRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    EntityManager em;

    // ─────────────────────────────────────────────────────────────────────────
    // CRUD EVENTI
    // ─────────────────────────────────────────────────────────────────────────

    /** Restituisce la lista degli eventi pubblici prossimi, filtrabili per tipo. */
    public List<EventDTO> getUpcomingEvents(String type) {
        return eventRepository.findUpcoming(type)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /** Restituisce il dettaglio di un evento con i partecipanti. */
    public EventDTO getById(UUID eventId) {
        Event event = eventRepository.findByIdWithParticipants(eventId)
                .orElseThrow(NotFoundException::new);
        return toDTOWithParticipants(event);
    }

    /** Restituisce gli eventi creati da un utente. */
    public List<EventDTO> getByCreator(UUID creatorId) {
        return eventRepository.findByCreator(creatorId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /** Restituisce gli eventi a cui un utente partecipa. */
    public List<EventDTO> getByParticipant(UUID userId) {
        return eventRepository.findByParticipant(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /** Crea un nuovo evento. */
    public EventDTO create(UUID creatorId, EventDTO dto) {
        log.info("Creating event '{}' by user {}", dto.getTitle(), creatorId);

        User creator = userRepository.findById(creatorId);
        if (creator == null) throw new NotFoundException("Creator not found");

        Event event = new Event();
        event.setCreator(creator);
        event.setType(dto.getType());
        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setStartsAt(dto.getStartsAt());
        event.setEndsAt(dto.getEndsAt());
        event.setMaxParticipants(dto.getMaxParticipants());
        event.setStatus(dto.getStatus() != null ? dto.getStatus() : "OPEN");
        event.setVisibility(dto.getVisibility() != null ? dto.getVisibility() : "PUBLIC");
        event.setTags(dto.getTags());
        event.setCreatedAt(OffsetDateTime.now());
        event.setUpdatedAt(OffsetDateTime.now());

        if (dto.getLocationId() != null) {
            EventLocation loc = locationRepository.findById(dto.getLocationId());
            if (loc == null) throw new NotFoundException("Location not found");
            event.setLocation(loc);
        }

        eventRepository.persist(event);

        // Il creatore viene aggiunto automaticamente come partecipante GOING
        addParticipantInternal(event, creator, "GOING");

        log.info("Event created with id {}", event.getId());
        return toDTO(event);
    }

    /** Aggiorna un evento esistente. Solo il creatore può modificarlo. */
    public EventDTO update(UUID creatorId, UUID eventId, EventDTO dto) {
        Event event = eventRepository.findActiveById(eventId)
                .orElseThrow(NotFoundException::new);

        if (!event.getCreator().getId().equals(creatorId)) {
            throw new ForbiddenException("Only the creator can update this event");
        }

        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setStartsAt(dto.getStartsAt());
        event.setEndsAt(dto.getEndsAt());
        event.setMaxParticipants(dto.getMaxParticipants());
        event.setVisibility(dto.getVisibility());
        event.setTags(dto.getTags());
        event.setUpdatedAt(OffsetDateTime.now());

        if (dto.getStatus() != null) {
            event.setStatus(dto.getStatus());
        }

        if (dto.getLocationId() != null) {
            EventLocation loc = locationRepository.findById(dto.getLocationId());
            if (loc == null) throw new NotFoundException("Location not found");
            event.setLocation(loc);
        }

        return toDTO(event);
    }

    /**
     * Soft-delete di un evento. Solo il creatore può eliminarlo.
     * Non elimina fisicamente: imposta deletedAt.
     */
    public void delete(UUID creatorId, UUID eventId) {
        Event event = eventRepository.findActiveById(eventId)
                .orElseThrow(NotFoundException::new);

        if (!event.getCreator().getId().equals(creatorId)) {
            throw new ForbiddenException("Only the creator can delete this event");
        }

        event.setDeletedAt(OffsetDateTime.now());
        event.setStatus("CANCELLED");
        event.setUpdatedAt(OffsetDateTime.now());
        log.info("Event {} soft-deleted by {}", eventId, creatorId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PARTECIPAZIONI / RSVP
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Un utente si iscrive autonomamente a un evento (GOING).
     * Controlla che l'evento non sia pieno e che l'utente non sia già iscritto.
     */
    public EventParticipantDTO join(UUID userId, UUID eventId) {
        Event event = eventRepository.findActiveById(eventId)
                .orElseThrow(NotFoundException::new);

        if ("FULL".equals(event.getStatus()) || "CANCELLED".equals(event.getStatus())) {
            throw new BadRequestException("Event is not open for new participants");
        }

        if (participantRepository.existsByEventAndUser(eventId, userId)) {
            throw new BadRequestException("User is already registered for this event");
        }

        User user = userRepository.findById(userId);
        if (user == null) throw new NotFoundException("User not found");

        EventParticipant participant = addParticipantInternal(event, user, "GOING");

        // Aggiorna lo status a FULL se necessario
        if (event.getMaxParticipants() != null) {
            long goingCount = participantRepository.countGoingByEvent(eventId);
            if (goingCount >= event.getMaxParticipants()) {
                event.setStatus("FULL");
                event.setUpdatedAt(OffsetDateTime.now());
            }
        }

        return toParticipantDTO(participant);
    }

    /**
     * Il creatore invita esplicitamente un utente (INVITED).
     */
    public EventParticipantDTO invite(UUID creatorId, UUID eventId, UUID targetUserId) {
        Event event = eventRepository.findActiveById(eventId)
                .orElseThrow(NotFoundException::new);

        if (!event.getCreator().getId().equals(creatorId)) {
            throw new ForbiddenException("Only the creator can send invitations");
        }

        if (participantRepository.existsByEventAndUser(eventId, targetUserId)) {
            throw new BadRequestException("User is already registered for this event");
        }

        User target = userRepository.findById(targetUserId);
        if (target == null) throw new NotFoundException("User not found");

        EventParticipant participant = addParticipantInternal(event, target, "INVITED");
        return toParticipantDTO(participant);
    }

    /**
     * Un utente aggiorna il proprio RSVP (GOING, MAYBE, NOT_GOING).
     */
    public EventParticipantDTO updateRsvp(UUID userId, UUID eventId, String rsvpStatus) {
        EventParticipant participant = participantRepository.findByEventAndUser(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Participation not found"));

        if (!List.of("GOING", "MAYBE", "NOT_GOING").contains(rsvpStatus)) {
            throw new BadRequestException("Invalid rsvp_status: " + rsvpStatus);
        }

        participant.setRsvpStatus(rsvpStatus);
        participant.setRespondedAt(OffsetDateTime.now());

        // Se era FULL e ora si libera un posto, torna OPEN
        Event event = participant.getEvent();
        if ("FULL".equals(event.getStatus()) && !"GOING".equals(rsvpStatus)) {
            event.setStatus("OPEN");
            event.setUpdatedAt(OffsetDateTime.now());
        }

        return toParticipantDTO(participant);
    }

    /**
     * Un utente abbandona l'evento (rimuove la propria partecipazione).
     */
    public void leave(UUID userId, UUID eventId) {
        EventParticipant participant = participantRepository.findByEventAndUser(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Participation not found"));

        Event event = participant.getEvent();

        participantRepository.delete(participant);

        // Se l'evento era FULL, riaprirlo
        if ("FULL".equals(event.getStatus())) {
            event.setStatus("OPEN");
            event.setUpdatedAt(OffsetDateTime.now());
        }

        log.info("User {} left event {}", userId, eventId);
    }

    /**
     * Lista partecipanti di un evento, opzionalmente filtrata per stato RSVP.
     */
    public List<EventParticipantDTO> getParticipants(UUID eventId, String rsvpStatus) {
        List<EventParticipant> participants = rsvpStatus != null
                ? participantRepository.findByEventAndStatus(eventId, rsvpStatus)
                : participantRepository.findByEvent(eventId);

        return participants.stream()
                .map(this::toParticipantDTO)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOCATION
    // ─────────────────────────────────────────────────────────────────────────

    /** Crea una nuova location. */
    public EventLocationDTO createLocation(UUID userId, EventLocationDTO dto) {
        User creator = userRepository.findById(userId);

        EventLocation location = new EventLocation();
        location.setCreatedBy(creator);
        location.setName(dto.getName());
        location.setAddress(dto.getAddress());
        location.setCity(dto.getCity());
        location.setLat(dto.getLat());
        location.setLng(dto.getLng());
        location.setCourtType(dto.getCourtType() != null ? dto.getCourtType() : "OUTDOOR");
        location.setIsIndoor(dto.getIsIndoor() != null ? dto.getIsIndoor() : false);
        location.setIsPublic(dto.getIsPublic() != null ? dto.getIsPublic() : true);
        location.setCreatedAt(OffsetDateTime.now());

        locationRepository.persist(location);
        log.info("EventLocation '{}' created with id {}", location.getName(), location.getId());
        return toLocationDTO(location);
    }

    /** Lista location pubbliche, opzionalmente filtrate per città. */
    public List<EventLocationDTO> getLocations(String city) {
        List<EventLocation> locations = city != null
                ? locationRepository.findByCity(city)
                : locationRepository.findPublic();

        return locations.stream()
                .map(this::toLocationDTO)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MAPPER HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private EventParticipant addParticipantInternal(Event event, User user, String rsvpStatus) {
        EventParticipant participant = new EventParticipant();
        participant.setEvent(event);
        participant.setUser(user);
        participant.setRsvpStatus(rsvpStatus);
        participant.setRespondedAt("GOING".equals(rsvpStatus) ? OffsetDateTime.now() : null);
        participant.setJoinedAt(OffsetDateTime.now());
        participantRepository.persist(participant);
        return participant;
    }

    private EventDTO toDTO(Event e) {
        EventDTO dto = new EventDTO();
        dto.setId(e.getId());
        dto.setCreatorId(e.getCreator().getId());
        dto.setCreatorDisplayName(e.getCreator().getDisplayName());
        dto.setType(e.getType());
        dto.setTitle(e.getTitle());
        dto.setDescription(e.getDescription());
        dto.setStartsAt(e.getStartsAt());
        dto.setEndsAt(e.getEndsAt());
        dto.setMaxParticipants(e.getMaxParticipants());
        dto.setStatus(e.getStatus());
        dto.setVisibility(e.getVisibility());
        dto.setTags(e.getTags());
        dto.setCreatedAt(e.getCreatedAt());
        dto.setUpdatedAt(e.getUpdatedAt());
        if (e.getLocation() != null) {
            dto.setLocationId(e.getLocation().getId());
            dto.setLocation(toLocationDTO(e.getLocation()));
        }
        // Count GOING senza caricare la collection
        dto.setParticipantCount((int) participantRepository.countGoingByEvent(e.getId()));
        return dto;
    }

    private EventDTO toDTOWithParticipants(Event e) {
        EventDTO dto = toDTO(e);
        dto.setParticipants(
            e.getParticipants().stream()
                .map(this::toParticipantDTO)
                .collect(Collectors.toList())
        );
        return dto;
    }

    private EventParticipantDTO toParticipantDTO(EventParticipant p) {
        EventParticipantDTO dto = new EventParticipantDTO();
        dto.setId(p.getId());
        dto.setEventId(p.getEvent().getId());
        dto.setUserId(p.getUser().getId());
        dto.setUserDisplayName(p.getUser().getDisplayName());
        dto.setRsvpStatus(p.getRsvpStatus());
        dto.setRespondedAt(p.getRespondedAt());
        dto.setNote(p.getNote());
        dto.setJoinedAt(p.getJoinedAt());
        return dto;
    }

    private EventLocationDTO toLocationDTO(EventLocation l) {
        EventLocationDTO dto = new EventLocationDTO();
        dto.setId(l.getId());
        if (l.getCreatedBy() != null) dto.setCreatedById(l.getCreatedBy().getId());
        dto.setName(l.getName());
        dto.setAddress(l.getAddress());
        dto.setCity(l.getCity());
        dto.setLat(l.getLat());
        dto.setLng(l.getLng());
        dto.setCourtType(l.getCourtType());
        dto.setIsIndoor(l.getIsIndoor());
        dto.setIsPublic(l.getIsPublic());
        dto.setCreatedAt(l.getCreatedAt());
        return dto;
    }
}
