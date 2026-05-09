package com.mvpiq.repositories;

import com.mvpiq.model.Message;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class MessageRepository implements PanacheRepositoryBase<Message, UUID> {

    public List<Message> findByUserId(UUID userId) {
        return list(
                "sender.id = :uid OR conversation.id IN (" +
                        "select cp.conversation.id from ConversationParticipant cp where cp.user.id = :uid" +
                        ")",
                Parameters.with("uid", userId)
        );
    }

    public List<Message> findByConversationIdOrderByCreatedAtAsc(UUID conversationId) {
        return find("conversation.id = ?1 ORDER BY createdAt ASC", conversationId).list();
    }
}
