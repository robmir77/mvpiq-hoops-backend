package com.mvpiq.service.ia;

import ai.djl.modality.cv.output.Point;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Hoop {
    Point center;
    public double radius;

    public Hoop(int predictedX, int predictedY, int predictedRadius, int frameWidth, int frameHeight) {

        this.center = new Point(
                (double) predictedX / frameWidth,
                (double) predictedY / frameHeight
        );

        this.radius = (double) predictedRadius / frameWidth;
    }
}
