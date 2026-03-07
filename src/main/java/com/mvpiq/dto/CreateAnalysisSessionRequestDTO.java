package com.mvpiq.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
public class CreateAnalysisSessionRequestDTO {

    public UUID userId;

    public String analysisCode;

    public String videoUrl;

    public Integer videoSeconds;

    public Integer videoSizeMb;
}