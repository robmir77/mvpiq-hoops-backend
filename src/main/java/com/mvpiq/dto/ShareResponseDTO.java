package com.mvpiq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareResponseDTO {
    private String shareToken;
    private String shareUrl;
    private Boolean shareEnabled;
    private String publicSlug;
}
