package com.mvpiq.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OllamaResponseDTO {
    
    private String model;
    private String response;
    private Boolean done;
    private String created_at;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OllamaErrorDTO {
        private String error;
    }
}
