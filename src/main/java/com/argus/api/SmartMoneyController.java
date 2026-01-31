package com.argus.api;

import com.argus.api.dto.response.SmartMoneyResponse;
import com.argus.domain.model.SmartMoneyArchetype;
import com.argus.domain.model.WalletMetrics;
import com.argus.domain.port.persistence.WalletMetricsPersistencePort;
import com.argus.domain.service.SmartMoneyScoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.argus.api.spec.SmartMoneyApi;
import com.argus.core.exception.WalletNotFoundException;

@Slf4j
@RestController
@RequestMapping("/api/v1/smart-money")
@RequiredArgsConstructor
public class SmartMoneyController implements SmartMoneyApi {

    private final WalletMetricsPersistencePort walletMetricsPort;
    private final SmartMoneyScoringService scoringService;

    @Override
    public ResponseEntity<Page<SmartMoneyResponse>> getAllMetrics(
            int page,
            int size,
            SmartMoneyArchetype archetype) {

        log.info("GET /api/v1/smart-money - page: {}, size: {}, archetype: {}", page, size, archetype);

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "totalScore"));

        Page<WalletMetrics> result;
        if (archetype != null) {
            result = walletMetricsPort.findByArchetype(archetype, pageRequest);
        } else {
            result = walletMetricsPort.findAll(pageRequest);
        }

        return ResponseEntity.ok(result.map(SmartMoneyResponse::fromDomain));
    }

    @Override
    public ResponseEntity<SmartMoneyResponse> getWalletMetrics(String address) {
        log.info("GET /api/v1/smart-money/{} - Fetching metrics", address);

        return walletMetricsPort.findByWalletAddress(address.toLowerCase())
                .map(SmartMoneyResponse::fromDomain)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new WalletNotFoundException(address));
    }

    @Override
    public ResponseEntity<List<SmartMoneyResponse>> getTopWallets(
            String sortBy,
            int limit) {

        log.info("GET /api/v1/smart-money/top - sortBy: {}, limit: {}", sortBy, limit);

        String field = switch (sortBy.toLowerCase()) {
            case "pnl" -> "pnlScore";
            case "consistency" -> "consistencyScore";
            case "conviction" -> "convictionScore";
            default -> "totalScore";
        };

        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, field));
        Page<WalletMetrics> result = walletMetricsPort.findAll(pageRequest);

        return ResponseEntity.ok(result.getContent().stream()
                .map(SmartMoneyResponse::fromDomain)
                .toList());
    }

    @Override
    public ResponseEntity<SmartMoneyResponse> refreshMetrics(String address) {
        log.info("POST /api/v1/smart-money/{}/refresh - Manually triggering refresh", address);

        return scoringService.calculateMetrics(address.toLowerCase())
                .map(SmartMoneyResponse::fromDomain)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new WalletNotFoundException(address));
    }
}
