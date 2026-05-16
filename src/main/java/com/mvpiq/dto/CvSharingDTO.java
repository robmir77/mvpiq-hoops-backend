package com.mvpiq.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CvSharingDTO {
    private String shareToken;
    private boolean shareEnabled;
    private String publicUrl;
}
