package com.argus.domain.port.persistence;

import com.argus.domain.model.AlertRule;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertRulePersistencePort {
    AlertRule save(AlertRule rule);

    Optional<AlertRule> findByIdAndUserId(UUID id, UUID userId);

    List<AlertRule> findByUserId(UUID userId);

    List<AlertRule> findAllEnabled();

    void deleteByIdAndUserId(UUID id, UUID userId);
}
