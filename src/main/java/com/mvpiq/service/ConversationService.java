package com.mvpiq.service;

import com.mvpiq.model.Conversation;
import com.mvpiq.model.ConversationParticipant;
import com.mvpiq.model.MediaAsset;
import com.mvpiq.model.Message;
import com.mvpiq.model.User;
import com.mvpiq.repositories.ConversationParticipantRepository;
import com.mvpiq.repositories.ConversationRepository;
import com.mvpiq.repositories.MessageRepository;
import com.mvpiq.repositories.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@ApplicationScoped
public class ConversationService {

    @Inject
    ConversationRepository conversationRepository;

    @Inject
    ConversationParticipantRepository participantRepository;

    @Inject
    MessageRepository messageRepository;

    @Inject
    UserRepository userRepository;

    @Transactional
    public Conversation createConversation(String title, List<UUID> participantIds) {
        log.info("Creating conversation with {} participants", participantIds.size());
        
        Conversation conversation = new Conversation();
        conversation.setTitle(title);
        conversation.setCreatedAt(OffsetDateTime.now());
        
        conversationRepository.persist(conversation);
        
        // Add participants
        for (UUID userId : participantIds) {
            User user = userRepository.findById(userId);
            if (user != null) {
                ConversationParticipant participant = new ConversationParticipant();
                participant.setConversation(conversation);
                participant.setUser(user);
                participant.setJoinedAt(OffsetDateTime.now());
                participantRepository.persist(participant);
            }
        }
        
        log.info("Conversation created with ID: {}", conversation.getId());
        return conversation;
    }

    @Transactional
    public Message sendMessage(UUID conversationId, UUID senderId, String content, String messageType, UUID mediaId) {
        log.info("Sending message to conversation: {} from user: {}", conversationId, senderId);
        
        Conversation conversation = conversationRepository.findById(conversationId);
        if (conversation == null) {
            throw new IllegalArgumentException("Conversation not found: " + conversationId);
        }
        
        // Verify sender is participant
        boolean isParticipant = participantRepository.existsByUserAndConversation(senderId, conversationId);
        if (!isParticipant) {
            throw new IllegalArgumentException("User is not a participant in this conversation");
        }
        
        User sender = userRepository.findById(senderId);
        if (sender == null) {
            throw new IllegalArgumentException("Sender not found: " + senderId);
        }
        
        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(content);
        message.setMessageType(messageType != null ? messageType : "text");
        message.setCreatedAt(OffsetDateTime.now());
        
        if (mediaId != null) {
            messageRepository.getEntityManager().getReference(MediaAsset.class, mediaId);
            // Note: You would need to inject MediaAssetRepository and set the media
        }
        
        messageRepository.persist(message);
        log.info("Message sent with ID: {}", message.getId());
        return message;
    }

    public List<Conversation> getUserConversations(UUID userId) {
        return conversationRepository.findByParticipantId(userId);
    }

    public Optional<Conversation> getConversation(UUID conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId);
        return Optional.ofNullable(conversation);
    }

    public List<Message> getConversationMessages(UUID conversationId) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    public List<ConversationParticipant> getConversationParticipants(UUID conversationId) {
        return participantRepository.findByConversationId(conversationId);
    }

    @Transactional
    public void addParticipant(UUID conversationId, UUID userId) {
        log.info("Adding participant {} to conversation {}", userId, conversationId);
        
        if (participantRepository.existsByUserAndConversation(userId, conversationId)) {
            throw new IllegalArgumentException("User is already a participant");
        }
        
        Conversation conversation = conversationRepository.findById(conversationId);
        User user = userRepository.findById(userId);
        
        if (conversation != null && user != null) {
            ConversationParticipant participant = new ConversationParticipant();
            participant.setConversation(conversation);
            participant.setUser(user);
            participant.setJoinedAt(OffsetDateTime.now());
            participantRepository.persist(participant);
        }
    }

    @Transactional
    public void removeParticipant(UUID conversationId, UUID userId) {
        log.info("Removing participant {} from conversation {}", userId, conversationId);
        
        ConversationParticipant participant = participantRepository.findByUserAndConversation(userId, conversationId);
        if (participant != null) {
            participantRepository.delete(participant);
        }
    }
}
