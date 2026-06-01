package com.mvpiq.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO per EventLocation.
 * Usato sia in input (creazione/aggiornamento) sia in output (risposta API).
 */
@Data
public class EventLocationDTO {

    private UUID id;

    /** UUID dell'utente che ha censito il luogo. */
    private UUID createdById;

    /** Nome del luogo (es. "Campetto Parco Sempione"). Obbligatorio in creazione. */
    private String name;

    /** Indirizzo esteso. */
    private String address;

    /** Città. */
    private String city;

    /** Latitudine (WGS84). */
    private Double lat;

    /** Longitudine (WGS84). */
    private Double lng;

    /** Tipo campo: OUTDOOR | INDOOR | GYM. Default: OUTDOOR. */
    private String courtType;

    /** True se il campo è al coperto. */
    private Boolean isIndoor;

    /** True se il campo è pubblicamente accessibile. */
    private Boolean isPublic;

    private OffsetDateTime createdAt;
}
