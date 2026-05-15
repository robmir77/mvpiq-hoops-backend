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
public class HighlightRequestDTO {
    private String title;
    private String description;
    private UUID mediaId;
    private String externalUrl;
    private String thumbnailUrl;
    private Integer sortOrder;
}
