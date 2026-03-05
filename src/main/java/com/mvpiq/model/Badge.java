package com.mvpiq.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

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
@Table(name = "badges")
public class Badge {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id = UUID.randomUUID();

    @Column(name = "code", length = 60, nullable = false, unique = true)
    @JsonProperty("code")
    private String code;

    @Column(name = "name", length = 150, nullable = false)
    @JsonProperty("name")
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    @JsonProperty("description")
    private String description;

    @Column(name = "points")
    @JsonProperty("points")
    private Integer points = 0;

    @Column(name = "icon_url", columnDefinition = "TEXT")
    @JsonProperty("icon_url")
    private String iconUrl;

    @Column(name = "category", length = 100)
    @JsonProperty("category")
    private String category;

    @Column(name = "created_at")
    @JsonProperty("created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
