package com.mvpiq.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class BallPointDTO {
    public double x;
    public double y;
    public int frame;

    public BallPointDTO(double x, double y) {
        this.x = x;
        this.y = y;
        this.frame = -1;
    }
}
