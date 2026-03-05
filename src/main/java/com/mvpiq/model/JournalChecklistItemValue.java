package com.mvpiq.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "journal_checklist_item_values")
public class JournalChecklistItemValue {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checklist_id", nullable = false)
    private JournalChecklist checklist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_item_id", nullable = false)
    private ChecklistTemplateItem templateItem;

    @Column(name = "boolean_value")
    private Boolean booleanValue;

    @Column(name = "number_value", precision = 15, scale = 2)
    private BigDecimal numberValue;

    @Column(name = "text_value", columnDefinition = "text")
    private String textValue;

    @Column(name = "select_value", length = 100)
    private String selectValue;

    @Column(name = "completed", nullable = false)
    private Boolean completed = false;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}