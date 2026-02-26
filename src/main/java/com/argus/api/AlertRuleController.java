package com.argus.api;

import com.argus.api.dto.request.AlertRuleRequest;
import com.argus.api.dto.response.AlertRuleResponse;
import com.argus.core.security.UserContext;
import com.argus.domain.model.AlertRule;
import com.argus.domain.service.AlertRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alert-rules")
@RequiredArgsConstructor
public class AlertRuleController {

    private final AlertRuleService alertRuleService;
    private final UserContext userContext;

    @PostMapping
    public ResponseEntity<AlertRuleResponse> createRule(@Valid @RequestBody AlertRuleRequest request) {
        UUID userId = userContext.getUserId();
        AlertRule rule = AlertRule.builder()
                .name(request.getName())
                .conditions(request.getConditions())
                .channels(request.getChannels())
                .enabled(request.isEnabled())
                .build();
        AlertRule created = alertRuleService.createRule(rule, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(AlertRuleResponse.from(created));
    }

    @GetMapping
    public ResponseEntity<List<AlertRuleResponse>> getRules() {
        UUID userId = userContext.getUserId();
        List<AlertRuleResponse> rules = alertRuleService.getRules(userId)
                .stream()
                .map(AlertRuleResponse::from)
                .toList();
        return ResponseEntity.ok(rules);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlertRuleResponse> getRule(@PathVariable UUID id) {
        UUID userId = userContext.getUserId();
        AlertRule rule = alertRuleService.getRule(id, userId);
        return ResponseEntity.ok(AlertRuleResponse.from(rule));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AlertRuleResponse> updateRule(
            @PathVariable UUID id,
            @Valid @RequestBody AlertRuleRequest request) {
        UUID userId = userContext.getUserId();
        AlertRule updated = AlertRule.builder()
                .name(request.getName())
                .conditions(request.getConditions())
                .channels(request.getChannels())
                .enabled(request.isEnabled())
                .build();
        AlertRule saved = alertRuleService.updateRule(id, updated, userId);
        return ResponseEntity.ok(AlertRuleResponse.from(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID id) {
        UUID userId = userContext.getUserId();
        alertRuleService.deleteRule(id, userId);
        return ResponseEntity.noContent().build();
    }
}
