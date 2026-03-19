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
    public int radius;

    public Hoop(int predictedX, int predictedY, int predictedRadius) {
        this.center = new Point(predictedX, predictedY);
        this.radius = predictedRadius;
    }
}
