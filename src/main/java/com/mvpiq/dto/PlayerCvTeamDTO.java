package com.mvpiq.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class PlayerCvTeamDTO {

    private UUID id;
    private String teamName;
    private Integer categoryId;
    private UUID positionId;
    private Integer startYear;
    private Integer endYear;
    private String notes;
}
