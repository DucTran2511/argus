package com.argus.domain.service;

import com.argus.domain.model.Alert;
import com.argus.domain.model.AlertRule;
import com.argus.domain.model.Signal;
import com.argus.domain.port.persistence.AlertPersistencePort;
import com.argus.domain.port.persistence.AlertRulePersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Core matching engine — called after every signal is saved.
 * Loads all enabled rules, tests each one, and persists an Alert for every
 * match.
 */
@Slf4j
@RequiredArgsConstructor
public class RuleMatcherService {

    private final AlertRulePersistencePort alertRulePort;
    private final AlertPersistencePort alertPort;

    public void matchSignal(Signal signal) {
        if (signal == null) {
            return;
        }

        List<AlertRule> enabledRules = alertRulePort.findAllEnabled();
        if (enabledRules.isEmpty()) {
            return;
        }

        for (AlertRule rule : enabledRules) {
            try {
                if (rule.getConditions() != null && rule.getConditions().matches(signal)) {
                    Alert alert = Alert.builder()
                            .userId(rule.getUserId())
                            .alertRuleId(rule.getId())
                            .signalId(signal.getId())
                            .status("PENDING")
                            .read(false)
                            .createdAt(LocalDateTime.now())
                            .build();
                    alertPort.save(alert);
                    log.info("🔔 Alert created for rule '{}' user={} signal={}",
                            rule.getName(), rule.getUserId(), signal.getId());
                }
            } catch (Exception e) {
                log.error("Failed to process rule '{}' for signal {}", rule.getName(), signal.getId(), e);
            }
        }
    }
}
