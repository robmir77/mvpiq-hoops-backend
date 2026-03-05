package com.mvpiq.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
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
@Table(name = "creator_certifications")
public class CreatorCertification {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id = UUID.randomUUID();

    @Column(name = "user_id", columnDefinition = "UUID", nullable = false)
    @JsonProperty("user_id")
    private UUID userId;

    @Column(name = "cert_body", length = 200)
    @JsonProperty("cert_body")
    private String certBody;

    @Column(name = "cert_level", length = 100)
    @JsonProperty("cert_level")
    private String certLevel;

    @Column(name = "cert_doc_url", columnDefinition = "TEXT")
    @JsonProperty("cert_doc_url")
    private String certDocUrl;

    @Column(name = "issued_at")
    @JsonProperty("issued_at")
    private LocalDate issuedAt;

    @Column(name = "verified")
    @JsonProperty("verified")
    private Boolean verified = false;

    @Column(name = "verified_by", columnDefinition = "UUID")
    @JsonProperty("verified_by")
    private UUID verifiedBy;

    @Column(name = "verified_at")
    @JsonProperty("verified_at")
    private OffsetDateTime verifiedAt;
}
