package com.mvpiq.repositories;

import com.mvpiq.model.JournalEntry;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class JournalEntryRepository implements PanacheRepository<JournalEntry> {

    public List<JournalEntry> findByPlayer(UUID playerId) {
        return find("player.id = ?1 order by entryDate desc", playerId).list();
    }

    public Optional<JournalEntry> findByIdAndPlayer(UUID id, UUID playerId) {
        return find("id = ?1 and player.id = ?2", id, playerId).firstResultOptional();
    }
}
