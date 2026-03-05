package com.mvpiq.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class JournalEntryDTO {

    private UUID id;
    private String entryType;
    private String title;
    private String description;
    private OffsetDateTime entryDate;

    private String opponent;
    private String location;
    private Integer durationMinutes;

    private Short moodRating;
    private Short performanceRating;

    private String visibility;

    private List<JournalChecklistDTO> checklists;
}