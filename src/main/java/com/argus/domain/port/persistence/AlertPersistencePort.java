package com.argus.domain.port.persistence;

import com.argus.domain.model.Alert;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertPersistencePort {
    Alert save(Alert alert);

    List<Alert> findByUserId(UUID userId, int limit);

    Optional<Alert> findById(UUID id);

    void markAsRead(UUID id, UUID userId);

    long countUnreadByUserId(UUID userId);
}
