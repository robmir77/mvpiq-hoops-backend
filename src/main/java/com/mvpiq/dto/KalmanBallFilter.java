package com.mvpiq.dto;

public class KalmanBallFilter {

    private double x;
    private double y;
    private double vx;
    private double vy;

    private boolean initialized = false;

    public void init(double x, double y) {
        this.x = x;
        this.y = y;
        this.vx = 0;
        this.vy = 0;
        initialized = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    // predizione posizione nel frame successivo
    public void predict() {
        x += vx;
        y += vy;
    }

    // aggiorna filtro con misura YOLO
    public void update(double mx, double my) {

        double k = 0.6; // guadagno filtro

        double newX = k * mx + (1 - k) * x;
        double newY = k * my + (1 - k) * y;

        vx = newX - x;
        vy = newY - y;

        x = newX;
        y = newY;
    }

    public double getX() { return x; }
    public double getY() { return y; }
}