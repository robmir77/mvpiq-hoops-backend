package com.mvpiq.enums;

import java.util.Arrays;

public enum AnalisysType {

    BASKET_FREE_THROW,
    BASKET_THREE_POINT_SHOT,
    BASKET_MID_COURT,
    FITNESS_SQUAT,
    FITNESS_PUSHUP;

    /**
     * Conversione strict (lancia eccezione se non valido)
     */
    public static AnalisysType fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("AnalisysType is null");
        }
        return AnalisysType.valueOf(value.toUpperCase());
    }

    /**
     * Conversione safe (non lancia eccezioni)
     */
    public static AnalisysType fromStringSafe(String value) {
        if (value == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(v -> v.name().equalsIgnoreCase(value))
                .findFirst()
                .orElse(null);
    }

    /**
     * Conversione Optional (best practice moderna)
     */
    public static java.util.Optional<AnalisysType> fromStringOptional(String value) {
        if (value == null) {
            return java.util.Optional.empty();
        }
        return Arrays.stream(values())
                .filter(v -> v.name().equalsIgnoreCase(value))
                .findFirst();
    }
}
