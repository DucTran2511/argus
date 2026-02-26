package com.argus.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A triggered alert instance. Created when a signal matches a user's rule.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    private UUID id;
    private UUID userId;
    private UUID alertRuleId;
    private Long signalId;
    /** PENDING, SENT, DELIVERED, FAILED */
    private String status;
    private boolean read;
    private LocalDateTime createdAt;
}
