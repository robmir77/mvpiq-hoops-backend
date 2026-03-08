package com.mvpiq.dto;

import com.mvpiq.enums.MediaType;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaAssetDTO {

    private UUID id;
    private String title;
    private String description;
    private String mediaType;
    private String storageUrl;
    private String thumbnailUrl;
    private Integer durationSeconds;
    private Integer width;
    private Integer height;
    private Boolean isOfficial;
}
