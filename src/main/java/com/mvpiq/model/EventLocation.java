package com.mvpiq.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Luogo fisico (campo, palestra, parco) dove si svolge un evento.
 * Separato da Event per consentire il riutilizzo dello stesso posto in più eventi.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "event_locations")
public class EventLocation {

    @Id
    @GeneratedValue
    private UUID id;

    /** Utente che ha censito il luogo. NULL = inserimento di sistema. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    /** Nome descrittivo del luogo (es. "Campetto Parco Sempione"). */
    @Column(name = "name", length = 150, nullable = false)
    private String name;

    /** Indirizzo esteso. */
    @Column(name = "address", length = 255)
    private String address;

    /** Città. */
    @Column(name = "city", length = 100)
    private String city;

    /** Latitudine in gradi decimali (WGS84). */
    @Column(name = "lat")
    private Double lat;

    /** Longitudine in gradi decimali (WGS84). */
    @Column(name = "lng")
    private Double lng;

    /** Tipo di campo: OUTDOOR | INDOOR | GYM. */
    @Column(name = "court_type", length = 20, nullable = false)
    private String courtType = "OUTDOOR";

    /** True se il campo è al coperto. */
    @Column(name = "is_indoor", nullable = false)
    private Boolean isIndoor = false;

    /** True se il luogo è liberamente accessibile (parco, campo pubblico). */
    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = true;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
