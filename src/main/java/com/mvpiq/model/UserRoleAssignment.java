package com.mvpiq.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_roles",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "role_id"})
    }
)
public class UserRoleAssignment {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "assigned_at", updatable = false)
    private OffsetDateTime assignedAt;

    @PrePersist
    protected void onCreate() {
        this.assignedAt = OffsetDateTime.now();
    }
}
