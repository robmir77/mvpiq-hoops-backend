package com.mvpiq.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class AnalysisResultDTO {
    public UUID sessionId;
    public Integer score;

    public List<String> detectedErrors;
    public List<String> suggestions;
}