package com.mvpiq.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class JournalChecklistDTO {

    private UUID id;
    private UUID templateId;
    private String status;

    private List<JournalChecklistItemValueDTO> items;
}