package com.mvpiq.resource;

import com.mvpiq.dto.ApiResponse;
import com.mvpiq.model.Conversation;
import com.mvpiq.model.Message;
import com.mvpiq.service.ConversationService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Path("/api/conversations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Slf4j
@Authenticated
public class ConversationResource {

    @Inject
    ConversationService conversationService;

    @POST
    public Response createConversation(Map<String, Object> request) {
        try {
            String title = (String) request.get("title");
            @SuppressWarnings("unchecked")
            List<UUID> participantIds = (List<UUID>) request.get("participantIds");
            
            Conversation created = conversationService.createConversation(title, participantIds);
            return Response.status(Response.Status.CREATED)
                    .entity(ApiResponse.success(created, "Conversation created successfully"))
                    .build();
        } catch (Exception e) {
            log.error("Error creating conversation", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Error creating conversation: " + e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/{id}/messages")
    public Response sendMessage(@PathParam("id") UUID id, Map<String, Object> messageRequest) {
        try {
            UUID senderId = UUID.fromString(messageRequest.get("senderId").toString());
            String content = (String) messageRequest.get("content");
            String messageType = (String) messageRequest.getOrDefault("messageType", "text");
            UUID mediaId = messageRequest.get("mediaId") != null ? 
                UUID.fromString(messageRequest.get("mediaId").toString()) : null;
            
            Message message = conversationService.sendMessage(id, senderId, content, messageType, mediaId);
            return Response.status(Response.Status.CREATED)
                    .entity(ApiResponse.success(message, "Message sent successfully"))
                    .build();
        } catch (Exception e) {
            log.error("Error sending message", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Error sending message: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{id}")
    public Response getConversation(@PathParam("id") UUID id) {
        try {
            Optional<Conversation> conversation = conversationService.getConversation(id);
            if (conversation.isPresent()) {
                return Response.ok(ApiResponse.success(conversation.get(), "Conversation retrieved successfully")).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(ApiResponse.error("Conversation not found"))
                        .build();
            }
        } catch (Exception e) {
            log.error("Error retrieving conversation", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error retrieving conversation: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{id}/messages")
    public Response getConversationMessages(@PathParam("id") UUID id) {
        try {
            List<Message> messages = conversationService.getConversationMessages(id);
            return Response.ok(ApiResponse.success(messages, "Messages retrieved successfully")).build();
        } catch (Exception e) {
            log.error("Error retrieving messages", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error retrieving messages: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{id}/participants")
    public Response getConversationParticipants(@PathParam("id") UUID id) {
        try {
            List<?> participants = conversationService.getConversationParticipants(id);
            return Response.ok(ApiResponse.success(participants, "Participants retrieved successfully")).build();
        } catch (Exception e) {
            log.error("Error retrieving participants", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error retrieving participants: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/user/{userId}")
    public Response getUserConversations(@PathParam("userId") UUID userId) {
        try {
            List<Conversation> conversations = conversationService.getUserConversations(userId);
            return Response.ok(ApiResponse.success(conversations, "User conversations retrieved successfully")).build();
        } catch (Exception e) {
            log.error("Error retrieving user conversations", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error retrieving conversations: " + e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/{id}/participants")
    public Response addParticipant(@PathParam("id") UUID conversationId, Map<String, Object> request) {
        try {
            UUID userId = UUID.fromString(request.get("userId").toString());
            conversationService.addParticipant(conversationId, userId);
            return Response.ok(ApiResponse.success("Participant added successfully")).build();
        } catch (Exception e) {
            log.error("Error adding participant", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Error adding participant: " + e.getMessage()))
                    .build();
        }
    }

    @DELETE
    @Path("/{id}/participants/{userId}")
    public Response removeParticipant(@PathParam("id") UUID conversationId, @PathParam("userId") UUID userId) {
        try {
            conversationService.removeParticipant(conversationId, userId);
            return Response.ok(ApiResponse.success("Participant removed successfully")).build();
        } catch (Exception e) {
            log.error("Error removing participant", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Error removing participant: " + e.getMessage()))
                    .build();
        }
    }
}
