package com.mvpiq.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
@Table(name = "official_feed_items")
public class OfficialFeedItem {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id = UUID.randomUUID();

    @Column(name = "author_id", columnDefinition = "UUID")
    @JsonProperty("author_id")
    private UUID authorId;

    @Column(name = "media_id", columnDefinition = "UUID")
    @JsonProperty("media_id")
    private UUID mediaId;

    @Column(name = "title", length = 200)
    @JsonProperty("title")
    private String title;

    @Column(name = "body", columnDefinition = "TEXT")
    @JsonProperty("body")
    private String body;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "jsonb")
    @JsonProperty("tags")
    private String tags;

    @Column(name = "is_active")
    @JsonProperty("is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    @JsonProperty("created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
