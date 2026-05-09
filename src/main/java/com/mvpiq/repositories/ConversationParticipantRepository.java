package com.mvpiq.repositories;

import com.mvpiq.model.ConversationParticipant;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ConversationParticipantRepository implements PanacheRepositoryBase<ConversationParticipant, UUID> {

    public List<ConversationParticipant> findByUserId(UUID userId) {
        return find("user.id", userId).list();
    }

    public List<ConversationParticipant> findByConversationId(UUID conversationId) {
        return find("conversation.id", conversationId).list();
    }

    public ConversationParticipant findByUserAndConversation(UUID userId, UUID conversationId) {
        return find("user.id = ?1 AND conversation.id = ?2", userId, conversationId).firstResult();
    }

    public boolean existsByUserAndConversation(UUID userId, UUID conversationId) {
        return count("user.id = ?1 AND conversation.id = ?2", userId, conversationId) > 0;
    }
}
