package com.mvpiq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PdfCvDTO {
    private UUID playerCvId;
    private String playerName;
    private String playerEmail;
    private String headline;
    private String summary;
    private Map<String, Object> stats;
    private List<PlayerCvTeamDTO> teams;
    private List<HighlightDTO> highlights;
    private String publicUrl;
    private String shareToken;
}
