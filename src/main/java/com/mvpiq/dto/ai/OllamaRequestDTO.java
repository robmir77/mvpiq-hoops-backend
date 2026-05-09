package com.mvpiq.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OllamaRequestDTO {
    
    private String model;
    private String prompt;
    private Boolean stream = false;
    private OllamaOptionsDTO options;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OllamaOptionsDTO {
        private Double temperature = 0.7;
        private Integer top_p = 90;
        private Integer max_tokens = 2000;
    }
}
