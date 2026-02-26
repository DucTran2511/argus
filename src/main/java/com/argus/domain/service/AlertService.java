package com.argus.domain.service;

import com.argus.core.exception.AlertNotFoundException;
import com.argus.domain.model.Alert;
import com.argus.domain.port.persistence.AlertPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

/**
 * Thin service wrapping {@link AlertPersistencePort} for use by
 * {@code AlertController}.
 * Follows the hexagonal pattern: Controller → Service → Port.
 */
@Slf4j
@RequiredArgsConstructor
public class AlertService {

    private final AlertPersistencePort alertPort;

    public List<Alert> getAlerts(UUID userId, int limit) {
        return alertPort.findByUserId(userId, limit);
    }

    public void markAsRead(UUID alertId, UUID userId) {
        // Verify the alert belongs to this user before marking
        alertPort.findById(alertId)
                .filter(a -> a.getUserId().equals(userId))
                .orElseThrow(() -> new AlertNotFoundException(alertId));
        alertPort.markAsRead(alertId, userId);
    }

    public long countUnread(UUID userId) {
        return alertPort.countUnreadByUserId(userId);
    }
}
