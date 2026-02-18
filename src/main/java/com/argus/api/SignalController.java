package com.argus.api;

import com.argus.api.dto.response.SignalResponse;
import com.argus.api.spec.SignalApi;
import com.argus.domain.port.persistence.SignalPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/signals")
@RequiredArgsConstructor
public class SignalController implements SignalApi {

    private final SignalPersistencePort signalPersistencePort;
    private final ObjectMapper objectMapper;

    @Override
    public ResponseEntity<List<SignalResponse>> getSignals(boolean includeMev, int limit) {
        log.info("GET /api/v1/signals - includeMev={}, limit={}", includeMev, limit);

        return ResponseEntity.ok(signalPersistencePort.findAll(includeMev, limit).stream()
                .map(signal -> SignalResponse.from(signal, objectMapper))
                .toList());
    }
}
