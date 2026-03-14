package com.mvpiq.dto;

import com.mvpiq.enums.MediaType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaUploadRequestDTO {

    private String title;
    private String description;
    private MediaType mediaType;
    private String storageUrl;
    private String thumbnailUrl;
    private Integer durationSeconds;
    private Integer width;
    private Integer height;
    private Boolean isOfficial;
    private String ownerId; // meglio string → sarà convertito in UUID nel service
}
