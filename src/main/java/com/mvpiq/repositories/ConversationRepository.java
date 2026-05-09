package com.mvpiq.repositories;

import com.mvpiq.model.Conversation;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ConversationRepository implements PanacheRepositoryBase<Conversation, UUID> {

    public List<Conversation> findByParticipantId(UUID userId) {
        return find("SELECT DISTINCT c FROM Conversation c JOIN c.participants p WHERE p.user.id = ?1", userId).list();
    }

    public List<Conversation> findByTitleContaining(String title) {
        return find("LOWER(title) LIKE LOWER(?1)", "%" + title + "%").list();
    }

    public Conversation findWithParticipants(UUID conversationId) {
        return find("SELECT DISTINCT c FROM Conversation c LEFT JOIN FETCH c.participants WHERE c.id = ?1", conversationId).firstResult();
    }
}
