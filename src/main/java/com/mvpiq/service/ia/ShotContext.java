package com.mvpiq.service.ia;

import ai.djl.modality.cv.output.Point;
import com.mvpiq.dto.BallPointDTO;
import com.mvpiq.dto.PoseFrameDTO;
import com.mvpiq.dto.ShotMetricsDTO;
import com.mvpiq.enums.HandSide;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;

import java.awt.geom.AffineTransform;
import java.io.File;
import java.util.List;

public class ShotContext {
    List<File> frames;

    Hoop hoop;
    int hoopX;
    int hoopY;
    int rimRadius;

    double pixelToCm;
    double hoopXCm;
    double hoopYCm;
    double rimRadiusCm;

    List<BallPointDTO> ballPositions;
    List<Point> ballPixelPositions;
    List<Point> ballCmPositions;

    List<PoseFrameDTO> poseFrames;
    public HandSide shootingHand;

    int startShotFrame;
    int releaseFrame;
    public ShotMetricsDTO metrics;

    Point startShotPointCm;
    Point releasePointCm;
    Point startShotPointPx;
    Point releasePointPx;

    List<Point> flightArcCm;
    List<Point> flightArcPx;

    PolynomialFunction realArcCm;
    PolynomialFunction realArcPx;
    PolynomialFunction idealArcCm;
    PolynomialFunction idealArcPx;

    public List<AffineTransform> frameTransforms;
    public boolean stabilized;
}
