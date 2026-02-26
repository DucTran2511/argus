package com.argus.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity mapping to the existing {@code alert_rules} table.
 * The {@code conditions} and {@code channels} columns are stored as JSON
 * strings
 * and converted via Jackson in the adapter.
 */
@Entity
@Table(name = "alert_rules")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "name", nullable = false)
    private String name;

    /** Serialized {@code AlertConditions} as JSON. */
    @Column(name = "conditions", columnDefinition = "jsonb")
    private String conditions;

    /** Serialized list of channel strings as JSON. */
    @Column(name = "channels", columnDefinition = "jsonb")
    private String channels;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    private void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null)
            createdAt = now;
        if (updatedAt == null)
            updatedAt = now;
    }

    @PreUpdate
    private void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
