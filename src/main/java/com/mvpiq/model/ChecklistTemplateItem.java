package com.mvpiq.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "checklist_template_items")
public class ChecklistTemplateItem {

    @Id
    @GeneratedValue
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ChecklistTemplate template;

    @Column(name = "label", length = 255, nullable = false)
    private String label;

    @Column(name = "data_type", length = 20, nullable = false)
    private String dataType; // BOOLEAN | NUMBER | TEXT | DATE | SELECT | MULTI_SELECT

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = false;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "select_source", columnDefinition = "TEXT")
    private String selectSource; // STATIC | POSITION_METADATA | PLAYER_POSITION | TRAINING_TYPE | SQL

    @Column(name = "select_query", columnDefinition = "TEXT")
    private String selectQuery; // Query SQL da eseguire quando select_source = 'SQL'

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @OneToMany(
            mappedBy = "templateItem",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("sortOrder ASC")
    @JsonIgnoreProperties({"templateItem"})
    private Set<ChecklistTemplateItemOption> options = new LinkedHashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChecklistTemplateItem that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}