package com.argus.infra.stream.consumer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.argus.core.constant.StreamKeys;
import com.argus.domain.model.dto.WhaleDetectionRequest;
import com.argus.domain.service.WhaleDetectorService;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionStreamConsumer implements StreamListener<String, MapRecord<String, String, String>> {

    private final RedisTemplate<String, String> redisTemplate;
    private final WhaleDetectorService whaleDetectorService;

    @PostConstruct
    public void initConsumerGroup() {
        try {
            redisTemplate.opsForStream().createGroup(
                    StreamKeys.TRANSACTION_STREAM,
                    StreamKeys.TX_PROCESSOR_GROUP);
            log.info("Created consumer group: {}", StreamKeys.TX_PROCESSOR_GROUP);
        } catch (Exception e) {
            log.debug("Consumer group already exists: {}", e.getMessage());
        }
    }

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        try {
            Map<String, String> fields = message.getValue();
            String txHash = fields.get("txHash");

            log.debug("Processing transaction for signals: {}", txHash);

            detectSignals(fields);

            redisTemplate.opsForStream().acknowledge(
                    StreamKeys.TRANSACTION_STREAM,
                    StreamKeys.TX_PROCESSOR_GROUP,
                    message.getId());

        } catch (Exception e) {
            log.error("Signal detection failed for message {}: {}",
                    message.getId(), e.getMessage());
        }
    }

    private void detectSignals(Map<String, String> fields) {
        WhaleDetectionRequest request = buildRequestFromFields(fields);

        whaleDetectorService.detectAndSaveWhaleSignal(request)
                .ifPresent(signal -> log.info("🐋 Signal detected: {} ${} confidence={}",
                        signal.getType(),
                        signal.getUsdValue(),
                        signal.getConfidenceScore()));
    }

    private WhaleDetectionRequest buildRequestFromFields(Map<String, String> fields) {
        return WhaleDetectionRequest.builder()
                .txHash(fields.get("txHash"))
                .from(fields.get("from"))
                .to(fields.get("to"))
                .walletAddress(fields.get("walletAddress"))
                .tokenAddress(fields.get("tokenAddress"))
                .tokenSymbol(fields.get("tokenSymbol"))
                .usdValue(parseBigDecimal(fields.get("usdValue")))
                .timestamp(parseTimestamp(fields.get("timestamp")))
                .build();
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.equals("null") || value.isEmpty())
            return null;
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long parseLong(String value) {
        if (value == null || value.equals("null") || value.isEmpty())
            return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDateTime parseTimestamp(String value) {
        if (value == null || value.equals("null") || value.isEmpty())
            return null;
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            return null;
        }
    }
}
