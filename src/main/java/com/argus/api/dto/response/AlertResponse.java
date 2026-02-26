package com.argus.api.dto.response;

import com.argus.domain.model.Alert;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class AlertResponse {

    UUID id;
    UUID userId;
    UUID alertRuleId;
    Long signalId;
    String status;
    boolean read;
    LocalDateTime createdAt;

    public static AlertResponse from(Alert alert) {
        return AlertResponse.builder()
                .id(alert.getId())
                .userId(alert.getUserId())
                .alertRuleId(alert.getAlertRuleId())
                .signalId(alert.getSignalId())
                .status(alert.getStatus())
                .read(alert.isRead())
                .createdAt(alert.getCreatedAt())
                .build();
    }
}
