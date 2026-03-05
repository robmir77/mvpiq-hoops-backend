package com.mvpiq.service;

import com.mvpiq.dto.JournalEntryDTO;
import com.mvpiq.model.*;
import com.mvpiq.repositories.ChecklistTemplateRepository;
import com.mvpiq.repositories.JournalEntryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class JournalService {

    @Inject
    JournalEntryRepository entryRepository;

    @Inject
    JournalMapper mapper;

    @Inject
    EntityManager em;

    @Inject
    ChecklistTemplateRepository templateRepository;

    public List<JournalEntryDTO> getPlayerEntries(UUID playerId) {
        return entryRepository.findByPlayer(playerId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    public JournalEntryDTO getById(UUID playerId, UUID entryId) {
        JournalEntry entry = entryRepository
                .findByIdAndPlayer(entryId, playerId)
                .orElseThrow(NotFoundException::new);

        return mapper.toDTO(entry);
    }

    public JournalEntryDTO create(UUID playerId, JournalEntryDTO dto) {

        Player player = em.getReference(Player.class, playerId);

        JournalEntry entry = new JournalEntry();
        entry.setPlayer(player);
        entry.setEntryType(dto.getEntryType());
        entry.setTitle(dto.getTitle());
        entry.setDescription(dto.getDescription());
        entry.setEntryDate(dto.getEntryDate());
        entry.setOpponent(dto.getOpponent());
        entry.setLocation(dto.getLocation());
        entry.setDurationMinutes(dto.getDurationMinutes());
        entry.setMoodRating(dto.getMoodRating());
        entry.setPerformanceRating(dto.getPerformanceRating());
        entry.setVisibility(dto.getVisibility());
        entry.setCreatedAt(OffsetDateTime.now());
        entry.setUpdatedAt(OffsetDateTime.now());

        // 🔥 AUTO GENERAZIONE CHECKLIST
        generateChecklists(entry);

        entryRepository.persist(entry);

        return mapper.toDTO(entry);
    }

    private void generateChecklists(JournalEntry entry) {

        List<ChecklistTemplate> templates =
                templateRepository.findActiveByType(entry.getEntryType());

        for (ChecklistTemplate template : templates) {

            JournalChecklist checklist = new JournalChecklist();
            checklist.setJournalEntry(entry);
            checklist.setTemplate(template);
            checklist.setStatus("IN_PROGRESS");
            checklist.setCreatedAt(OffsetDateTime.now());
            checklist.setUpdatedAt(OffsetDateTime.now());

            entry.getChecklists().add(checklist);

            // 🔥 Generazione item values
            for (ChecklistTemplateItem templateItem : template.getItems()) {

                JournalChecklistItemValue value = new JournalChecklistItemValue();
                value.setChecklist(checklist);
                value.setTemplateItem(templateItem);
                value.setCompleted(false);
                value.setCreatedAt(OffsetDateTime.now());
                value.setUpdatedAt(OffsetDateTime.now());

                checklist.getItemValues().add(value);
            }
        }
    }

    public void delete(UUID playerId, UUID entryId) {
        JournalEntry entry = entryRepository
                .findByIdAndPlayer(entryId, playerId)
                .orElseThrow(NotFoundException::new);

        entryRepository.delete(entry);
    }
}