package com.mvpiq.resource;

import com.mvpiq.dto.MessageDTO;
import com.mvpiq.model.Message;
import com.mvpiq.repositories.MessageRepository;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/api")
@RequestScoped
public class MessageResource {

    @Inject
    MessageRepository messageRepository;

    // Recupera tutti i messaggi di un utente (come sender o partecipante)
    @GET
    @Path("/messages/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMessagesByUser(@PathParam("userId") UUID userId) {
        List<Message> messages = messageRepository.findByUserId(userId);
        List<MessageDTO> dtoList = messages.stream()
                .map(MessageDTO::fromEntity)
                .collect(Collectors.toList());
        return Response.ok(dtoList).build();
    }

    // Crea un nuovo messaggio
    @POST
    @Transactional
    @Path("/message")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response sendMessage(MessageDTO dto) {
        Message message = dto.toEntity();
        messageRepository.persistAndFlush(message);
        return Response.ok(MessageDTO.fromEntity(message)).build();
    }
}
