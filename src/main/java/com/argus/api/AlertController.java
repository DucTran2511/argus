package com.argus.api;

import com.argus.api.dto.response.AlertResponse;
import com.argus.api.dto.response.UnreadCountResponse;
import com.argus.core.security.UserContext;
import com.argus.domain.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;
    private final UserContext userContext;

    @GetMapping
    public ResponseEntity<List<AlertResponse>> listAlerts(
            @RequestParam(defaultValue = "50") int limit) {
        UUID userId = userContext.getUserId();
        List<AlertResponse> alerts = alertService.getAlerts(userId, limit)
                .stream()
                .map(AlertResponse::from)
                .toList();
        return ResponseEntity.ok(alerts);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID id) {
        UUID userId = userContext.getUserId();
        alertService.markAsRead(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount() {
        UUID userId = userContext.getUserId();
        long count = alertService.countUnread(userId);
        return ResponseEntity.ok(UnreadCountResponse.of(count));
    }
}
