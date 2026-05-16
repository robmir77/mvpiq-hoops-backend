package com.mvpiq.dto;

import lombok.Data;

@Data
public class AddHighlightRequest {
    private String title;
    private String description;
    // Per upload diretto: mediaId già creato
    private String mediaId;
    // Per link esterno (YouTube, Vimeo)
    private String externalUrl;
}
