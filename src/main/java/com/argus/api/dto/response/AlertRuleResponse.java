package com.argus.api.dto.response;

import com.argus.domain.model.AlertConditions;
import com.argus.domain.model.AlertRule;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class AlertRuleResponse {

    UUID id;
    UUID userId;
    String name;
    AlertConditions conditions;
    List<String> channels;
    boolean enabled;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    public static AlertRuleResponse from(AlertRule rule) {
        return AlertRuleResponse.builder()
                .id(rule.getId())
                .userId(rule.getUserId())
                .name(rule.getName())
                .conditions(rule.getConditions())
                .channels(rule.getChannels())
                .enabled(rule.isEnabled())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }
}
