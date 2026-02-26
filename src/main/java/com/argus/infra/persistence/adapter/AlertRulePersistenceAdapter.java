package com.argus.infra.persistence.adapter;

import com.argus.domain.model.AlertRule;
import com.argus.domain.model.AlertConditions;
import com.argus.domain.port.persistence.AlertRulePersistencePort;
import com.argus.infra.persistence.entity.AlertRuleEntity;
import com.argus.infra.persistence.repository.AlertRuleRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertRulePersistenceAdapter implements AlertRulePersistencePort {

    private final AlertRuleRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public AlertRule save(AlertRule rule) {
        AlertRuleEntity entity = toEntity(rule);
        AlertRuleEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<AlertRule> findByIdAndUserId(UUID id, UUID userId) {
        return repository.findByIdAndUserId(id, userId).map(this::toDomain);
    }

    @Override
    public List<AlertRule> findByUserId(UUID userId) {
        return repository.findByUserId(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<AlertRule> findAllEnabled() {
        return repository.findByEnabledTrue().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteByIdAndUserId(UUID id, UUID userId) {
        repository.deleteByIdAndUserId(id, userId);
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private AlertRuleEntity toEntity(AlertRule rule) {
        return AlertRuleEntity.builder()
                .id(rule.getId())
                .userId(rule.getUserId())
                .name(rule.getName())
                .conditions(toJson(rule.getConditions()))
                .channels(toJson(rule.getChannels()))
                .enabled(rule.isEnabled())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }

    private AlertRule toDomain(AlertRuleEntity entity) {
        return AlertRule.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .name(entity.getName())
                .conditions(fromJson(entity.getConditions(), AlertConditions.class))
                .channels(fromJson(entity.getChannels(), new TypeReference<List<String>>() {
                }))
                .enabled(entity.isEnabled())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private String toJson(Object obj) {
        if (obj == null)
            return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize to JSON", e);
            return null;
        }
    }

    private <T> T fromJson(String json, Class<T> type) {
        if (json == null)
            return null;
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize JSON: {}", json, e);
            return null;
        }
    }

    private <T> T fromJson(String json, TypeReference<T> typeRef) {
        if (json == null)
            return null;
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize JSON list: {}", json, e);
            return null;
        }
    }
}
