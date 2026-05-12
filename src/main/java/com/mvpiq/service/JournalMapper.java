package com.mvpiq.service;

import com.mvpiq.dto.*;
import com.mvpiq.model.JournalChecklist;
import com.mvpiq.model.JournalChecklistItemValue;
import com.mvpiq.model.JournalEntry;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class JournalMapper {

    public JournalEntryDTO toDTO(JournalEntry entity) {
        JournalEntryDTO dto = new JournalEntryDTO();

        dto.setId(entity.getId());
        dto.setEntryType(entity.getEntryType());
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        dto.setEntryDate(entity.getEntryDate());
        dto.setOpponent(entity.getOpponent());
        dto.setLocation(entity.getLocation());
        dto.setDurationMinutes(entity.getDurationMinutes());
        dto.setMoodRating(entity.getMoodRating());
        dto.setPerformanceRating(entity.getPerformanceRating());
        dto.setVisibility(entity.getVisibility());

        if (entity.getChecklists() != null) {
            dto.setChecklists(
                    entity.getChecklists().stream()
                            .map(this::toChecklistDTO)
                            .toList()
            );
        }

        return dto;
    }

    private JournalChecklistDTO toChecklistDTO(JournalChecklist entity) {
        JournalChecklistDTO dto = new JournalChecklistDTO();
        dto.setId(entity.getId());
        dto.setTemplateId(entity.getTemplate().getId());
        dto.setStatus(entity.getStatus());

        if (entity.getItemValues() != null) {
            dto.setItems(
                    entity.getItemValues().stream()
                            .sorted((a, b) -> {
                                Integer sortOrderA = a.getTemplateItem().getSortOrder();
                                Integer sortOrderB = b.getTemplateItem().getSortOrder();
                                if (sortOrderA == null && sortOrderB == null) return 0;
                                if (sortOrderA == null) return 1;
                                if (sortOrderB == null) return -1;
                                return sortOrderA.compareTo(sortOrderB);
                            })
                            .map(this::toItemDTO)
                            .toList()
            );
        }

        return dto;
    }

    private JournalChecklistItemValueDTO toItemDTO(JournalChecklistItemValue entity) {
        JournalChecklistItemValueDTO dto = new JournalChecklistItemValueDTO();
        dto.setId(entity.getId());
        dto.setTemplateItemId(entity.getTemplateItem().getId());
        dto.setBooleanValue(entity.getBooleanValue());
        dto.setNumberValue(entity.getNumberValue());
        dto.setTextValue(entity.getTextValue());
        dto.setSelectValue(entity.getSelectValue());
        dto.setCompleted(entity.getCompleted());
        return dto;
    }
}