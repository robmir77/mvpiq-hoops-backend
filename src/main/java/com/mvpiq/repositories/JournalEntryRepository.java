package com.mvpiq.repositories;

import com.mvpiq.model.JournalEntry;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class JournalEntryRepository implements PanacheRepositoryBase<JournalEntry, UUID> {

    public List<JournalEntry> findByPlayer(UUID playerId) {
        return find("player.id = ?1 order by entryDate desc", playerId).list();
    }

    public Optional<JournalEntry> findByIdAndPlayer(UUID id, UUID playerId) {
        return find("id = ?1 and player.id = ?2", id, playerId).firstResultOptional();
    }

    public List<JournalEntry> findByPlayerIdAndEntryDateBetweenOrderByEntryDateDesc(UUID playerId, OffsetDateTime startDate, OffsetDateTime endDate) {
        return find("player.id = ?1 AND entryDate BETWEEN ?2 AND ?3 ORDER BY entryDate DESC", playerId, startDate, endDate).list();
    }
}
