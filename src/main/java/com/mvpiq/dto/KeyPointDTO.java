package com.mvpiq.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class KeyPointDTO {
    private double x;
    private double y;
    private double confidence;

    public boolean isValid(double minConfidence) {
        return confidence >= minConfidence;
    }
}
