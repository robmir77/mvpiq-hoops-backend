package com.mvpiq.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class AnalysisResultDTO {

    public UUID sessionId;
    public Integer score;
    public String detectedErrors;
    public String suggestions;

}