package com.mvpiq.dto;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CreateAnalysisSessionRequestDTO {

    private String analysisCode;

    private String videoUrl;

    private Integer videoSeconds;

    private Integer videoSizeMb;
}