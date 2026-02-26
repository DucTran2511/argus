package com.argus.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Domain model for a user-defined alert rule, mapping to the existing
 * {@code alert_rules} table.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRule {

    private UUID id;
    private UUID userId;
    private String name;
    private AlertConditions conditions;
    private List<String> channels;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
