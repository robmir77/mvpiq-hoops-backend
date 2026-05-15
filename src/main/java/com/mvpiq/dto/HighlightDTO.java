package com.mvpiq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HighlightDTO {
    private UUID id;
    private String title;
    private String description;
    private UUID mediaId;
    private String externalUrl;
    private String thumbnailUrl;
    private Integer sortOrder;
    private String videoUrl;
    private String mimeType;
    private Long fileSizeBytes;
    private Integer durationSeconds;
}
