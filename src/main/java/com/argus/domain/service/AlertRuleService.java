package com.argus.domain.service;

import com.argus.core.exception.AlertRuleNotFoundException;
import com.argus.domain.model.AlertConditions;
import com.argus.domain.model.AlertRule;
import com.argus.domain.port.persistence.AlertRulePersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class AlertRuleService {

    private final AlertRulePersistencePort alertRulePort;

    public AlertRule createRule(AlertRule rule, UUID userId) {
        AlertRule toSave = AlertRule.builder()
                .userId(userId)
                .name(rule.getName())
                .conditions(rule.getConditions())
                .channels(rule.getChannels())
                .enabled(rule.isEnabled())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        AlertRule saved = alertRulePort.save(toSave);
        log.info("Created alert rule '{}' for user {}", saved.getName(), userId);
        return saved;
    }

    public List<AlertRule> getRules(UUID userId) {
        return alertRulePort.findByUserId(userId);
    }

    public AlertRule getRule(UUID id, UUID userId) {
        return alertRulePort.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new AlertRuleNotFoundException(id));
    }

    public AlertRule updateRule(UUID id, AlertRule updated, UUID userId) {
        AlertRule existing = getRule(id, userId);
        AlertRule toSave = AlertRule.builder()
                .id(existing.getId())
                .userId(userId)
                .name(updated.getName())
                .conditions(updated.getConditions())
                .channels(updated.getChannels())
                .enabled(updated.isEnabled())
                .createdAt(existing.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();
        return alertRulePort.save(toSave);
    }

    public void deleteRule(UUID id, UUID userId) {
        // Verify ownership before deleting
        getRule(id, userId);
        alertRulePort.deleteByIdAndUserId(id, userId);
        log.info("Deleted alert rule {} for user {}", id, userId);
    }

    /**
     * Provisions a sensible default rule for a new user: WHALE_BUY >= $50K.
     */
    public void createDefaultRulesForUser(UUID userId) {
        AlertConditions defaultConditions = AlertConditions.builder()
                .signalTypes(List.of("WHALE_BUY"))
                .minAmountUsd(new java.math.BigDecimal("50000"))
                .build();

        AlertRule defaultRule = AlertRule.builder()
                .userId(userId)
                .name("Whale Buys > $50K")
                .conditions(defaultConditions)
                .channels(List.of("in_app"))
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        alertRulePort.save(defaultRule);
        log.info("Provisioned default alert rule for user {}", userId);
    }
}
