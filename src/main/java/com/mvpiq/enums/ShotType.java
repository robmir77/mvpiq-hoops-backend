package com.mvpiq.enums;

public enum ShotType {

    // =========================
    // 🎯 BASIC SHOTS
    // =========================
    FREE_THROW(4.6),
    THREE_POINT(6.75),
    MID_COURT(14.0),

    // =========================
    // 🎯 EXTENDED (OPZIONALI)
    // =========================
    LONG_THREE(7.5),        // step dietro la linea
    CLOSE_RANGE(2.0),       // sotto canestro

    // =========================
    // 🎯 UNKNOWN / AUTO
    // =========================
    AUTO(-1);

    private final double defaultDistanceMeters;

    ShotType(double defaultDistanceMeters) {
        this.defaultDistanceMeters = defaultDistanceMeters;
    }

    public double getDefaultDistanceMeters() {
        return defaultDistanceMeters;
    }

    public boolean isAuto() {
        return this == AUTO;
    }
}
