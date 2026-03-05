package com.mvpiq.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder
@Entity
@Table(name = "creator_revenue_share")
public class CreatorRevenueShare {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id = UUID.randomUUID();

    @Column(name = "creator_id", columnDefinition = "UUID", nullable = false)
    @JsonProperty("creator_id")
    private UUID creatorId;

    @Column(name = "percent", precision = 5, scale = 2, nullable = false)
    @JsonProperty("percent")
    private BigDecimal percent = new BigDecimal("70.00");

    @Column(name = "effective_from")
    @JsonProperty("effective_from")
    private OffsetDateTime effectiveFrom = OffsetDateTime.now();

    @Column(name = "effective_to")
    @JsonProperty("effective_to")
    private OffsetDateTime effectiveTo;
}
