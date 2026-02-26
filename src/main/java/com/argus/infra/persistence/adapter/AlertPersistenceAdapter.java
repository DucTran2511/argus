package com.argus.infra.persistence.adapter;

import com.argus.core.exception.AlertNotFoundException;
import com.argus.domain.model.Alert;
import com.argus.domain.port.persistence.AlertPersistencePort;
import com.argus.infra.persistence.entity.AlertEntity;
import com.argus.infra.persistence.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AlertPersistenceAdapter implements AlertPersistencePort {

    private final AlertRepository repository;

    @Override
    public Alert save(Alert alert) {
        AlertEntity entity = toEntity(alert);
        return toDomain(repository.save(entity));
    }

    @Override
    public List<Alert> findByUserId(UUID userId, int limit) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, limit))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Alert> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public void markAsRead(UUID id, UUID userId) {
        int updated = repository.markAsRead(id, userId);
        if (updated == 0) {
            throw new AlertNotFoundException(id);
        }
    }

    @Override
    public long countUnreadByUserId(UUID userId) {
        return repository.countByUserIdAndIsReadFalse(userId);
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private AlertEntity toEntity(Alert alert) {
        return AlertEntity.builder()
                .id(alert.getId())
                .userId(alert.getUserId())
                .alertRuleId(alert.getAlertRuleId())
                .signalId(alert.getSignalId())
                .status(alert.getStatus())
                .isRead(alert.isRead())
                .createdAt(alert.getCreatedAt())
                .build();
    }

    private Alert toDomain(AlertEntity entity) {
        return Alert.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .alertRuleId(entity.getAlertRuleId())
                .signalId(entity.getSignalId())
                .status(entity.getStatus())
                .read(entity.isRead())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
