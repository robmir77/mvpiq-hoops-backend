package com.mvpiq.service;

import com.mvpiq.dto.JournalChecklistDTO;
import com.mvpiq.dto.JournalChecklistItemValueDTO;
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

    public List<JournalEntryDTO> getPlayerEntries(UUID playerId, String entryType) {
        List<JournalEntry> entries;
        if (entryType != null && !entryType.isBlank()) {
            entries = entryRepository.findByPlayerAndType(playerId, entryType);
        } else {
            entries = entryRepository.findByPlayer(playerId);
        }
        return entries.stream()
                .map(mapper::toDTO)
                .toList();
    }

    public JournalEntryDTO getById(UUID playerId, UUID entryId) {
        System.out.println("🔥 GET JOURNAL - EntryId: " + entryId + ", PlayerId: " + playerId);

        JournalEntry entry = entryRepository
                .findByIdAndPlayerWithChecklists(entryId, playerId)
                .orElseThrow(NotFoundException::new);

        System.out.println("🔥 Entry loaded, checklists: " + (entry.getChecklists() != null ? entry.getChecklists().size() : "null"));

        if (entry.getChecklists() != null) {
            for (var checklist : entry.getChecklists()) {
                System.out.println("🔥 Checklist ID: " + checklist.getId() + ", itemValues: " + (checklist.getItemValues() != null ? checklist.getItemValues().size() : "null"));
                if (checklist.getItemValues() != null) {
                    for (var itemValue : checklist.getItemValues()) {
                        System.out.println("🔥 ItemValue ID: " + itemValue.getId() + ", number: " + itemValue.getNumberValue() + ", text: " + itemValue.getTextValue() + ", boolean: " + itemValue.getBooleanValue() + ", select: " + itemValue.getSelectValue());
                    }
                }
            }
        }

        return mapper.toDTO(entry);
    }

    public JournalEntryDTO create(UUID playerId, JournalEntryDTO dto) {
        System.out.println("🔥 CREATE JOURNAL - EntryType: " + dto.getEntryType() + ", Checklists: " + (dto.getChecklists() != null ? dto.getChecklists().size() : "null"));
        
        // Log dettagliato del DTO ricevuto
        if (dto.getChecklists() != null) {
            for (JournalChecklistDTO cl : dto.getChecklists()) {
                System.out.println("🔥 DTO Checklist - TemplateId: " + cl.getTemplateId() + ", Items count: " + (cl.getItems() != null ? cl.getItems().size() : "null"));
                if (cl.getItems() != null) {
                    for (JournalChecklistItemValueDTO item : cl.getItems()) {
                        System.out.println("🔥 DTO Item - templateItemId: " + item.getTemplateItemId() + ", numberValue: " + item.getNumberValue() + ", textValue: " + item.getTextValue() + ", booleanValue: " + item.getBooleanValue() + ", selectValue: " + item.getSelectValue() + ", completed: " + item.getCompleted());
                    }
                }
            }
        }

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

        // 🔥 AUTO GENERAZIONE CHECKLIST con valori dal DTO se presenti
        generateChecklists(entry, dto.getChecklists());

        entryRepository.persist(entry);

        return mapper.toDTO(entry);
    }

    private void generateChecklists(JournalEntry entry, List<JournalChecklistDTO> dtoChecklists) {

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

            // 🔥 Generazione item values con valori dal DTO se presenti
            for (ChecklistTemplateItem templateItem : template.getItems()) {

                JournalChecklistItemValue value = new JournalChecklistItemValue();
                value.setChecklist(checklist);
                value.setTemplateItem(templateItem);
                value.setCompleted(false);
                value.setCreatedAt(OffsetDateTime.now());
                value.setUpdatedAt(OffsetDateTime.now());

                // Cerca valore corrispondente nel DTO
                if (dtoChecklists != null) {
                    for (JournalChecklistDTO dtoChecklist : dtoChecklists) {
                        if (dtoChecklist.getItems() != null) {
                            for (JournalChecklistItemValueDTO dtoItem : dtoChecklist.getItems()) {
                                if (dtoItem.getTemplateItemId().equals(templateItem.getId())) {
                                    System.out.println("🔥 Setting value for templateItem " + templateItem.getId() + ": number=" + dtoItem.getNumberValue() + ", text=" + dtoItem.getTextValue() + ", boolean=" + dtoItem.getBooleanValue() + ", select=" + dtoItem.getSelectValue() + ", completed=" + dtoItem.getCompleted());
                                    value.setBooleanValue(dtoItem.getBooleanValue());
                                    value.setNumberValue(dtoItem.getNumberValue());
                                    value.setTextValue(dtoItem.getTextValue());
                                    value.setSelectValue(dtoItem.getSelectValue());
                                    value.setCompleted(dtoItem.getCompleted());
                                    break;
                                }
                            }
                        }
                    }
                }

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

    public JournalEntryDTO update(UUID playerId, UUID entryId, JournalEntryDTO dto) {
        System.out.println("🔥 UPDATE JOURNAL - EntryId: " + entryId + ", PlayerId: " + playerId);
        System.out.println("🔥 DTO Checklists: " + (dto.getChecklists() != null ? dto.getChecklists().size() : "null"));

        JournalEntry entry = entryRepository
                .findByIdAndPlayerWithChecklists(entryId, playerId)
                .orElseThrow(NotFoundException::new);

        // Aggiorna campi base
        entry.setTitle(dto.getTitle());
        entry.setDescription(dto.getDescription());
        entry.setEntryDate(dto.getEntryDate());
        entry.setOpponent(dto.getOpponent());
        entry.setLocation(dto.getLocation());
        entry.setDurationMinutes(dto.getDurationMinutes());
        entry.setMoodRating(dto.getMoodRating());
        entry.setPerformanceRating(dto.getPerformanceRating());
        entry.setVisibility(dto.getVisibility());
        entry.setUpdatedAt(OffsetDateTime.now());

        // Aggiorna i valori delle checklist
        if (dto.getChecklists() != null) {
            for (JournalChecklistDTO checklistDTO : dto.getChecklists()) {
                System.out.println("🔥 Checklist ID: " + checklistDTO.getId() + ", Items: " + (checklistDTO.getItems() != null ? checklistDTO.getItems().size() : "null"));
                if (checklistDTO.getItems() != null) {
                    for (JournalChecklistItemValueDTO itemDTO : checklistDTO.getItems()) {
                        System.out.println("🔥 Item ID: " + itemDTO.getId() + ", numberValue: " + itemDTO.getNumberValue() + ", textValue: " + itemDTO.getTextValue());
                        // Trova il valore esistente per ID
                        entry.getChecklists().stream()
                            .flatMap(c -> c.getItemValues().stream())
                            .filter(v -> v.getId().equals(itemDTO.getId()))
                            .findFirst()
                            .ifPresentOrElse(itemValue -> {
                                System.out.println("🔥 Found item in DB, updating values...");
                                itemValue.setBooleanValue(itemDTO.getBooleanValue());
                                itemValue.setNumberValue(itemDTO.getNumberValue());
                                itemValue.setTextValue(itemDTO.getTextValue());
                                itemValue.setSelectValue(itemDTO.getSelectValue());
                                itemValue.setCompleted(itemDTO.getCompleted());
                                itemValue.setUpdatedAt(OffsetDateTime.now());
                            }, () -> System.out.println("🔥 WARNING: Item " + itemDTO.getId() + " NOT FOUND in DB!"));
                    }
                }
            }
        }

        return mapper.toDTO(entry);
    }
}