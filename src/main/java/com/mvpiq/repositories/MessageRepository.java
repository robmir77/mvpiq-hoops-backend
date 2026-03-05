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
                "senderId = :uid OR conversationId IN (" +
                        "select cp.conversationId from ConversationParticipant cp where cp.userId = :uid" +
                        ")",
                Parameters.with("uid", userId)
        );
    }
}
