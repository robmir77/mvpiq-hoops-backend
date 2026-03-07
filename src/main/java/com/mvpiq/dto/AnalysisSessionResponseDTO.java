package com.mvpiq.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class AnalysisSessionResponseDTO {

    public UUID id;

    public String status;

    public String analysisCode;

    public Instant createdAt;
}