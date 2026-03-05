package com.mvpiq.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class JournalChecklistItemValueDTO {

    private UUID id;
    private UUID templateItemId;

    private Boolean booleanValue;
    private BigDecimal numberValue;
    private String textValue;
    private String selectValue;

    private Boolean completed;
}