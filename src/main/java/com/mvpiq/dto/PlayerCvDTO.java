package com.mvpiq.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Data
public class PlayerCvDTO {

    private String headline;
    private String summary;
    private Map<String, Object> stats;

    private List<PlayerCvTeamDTO> teams;
}
