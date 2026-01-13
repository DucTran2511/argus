package com.argus.infra.stream.consumer;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.argus.core.constant.StreamKeys;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionStreamConsumer implements StreamListener<String, MapRecord<String, String, String>> {

    private final RedisTemplate<String, String> redisTemplate;

    private static final BigDecimal WHALE_THRESHOLD = new BigDecimal("50000");

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
            String usdValueStr = fields.get("usdValue");

            log.debug("Processing transaction for signals: {}", txHash);

            detectSignals(fields);

            redisTemplate.opsForStream().acknowledge(
                    StreamKeys.TRANSACTION_STREAM,
                    StreamKeys.TX_PROCESSOR_GROUP,
                    message.getId());

        } catch (Exception e) {
            log.error("Signal detection failed for message {}: {}",
                    message.getId(), e.getMessage());
            redisTemplate.opsForStream().acknowledge(
                    StreamKeys.TRANSACTION_STREAM,
                    StreamKeys.TX_PROCESSOR_GROUP,
                    message.getId());
        }
    }

    private void detectSignals(Map<String, String> fields) {
        String txHash = fields.get("txHash");
        String usdValueStr = fields.get("usdValue");
        String from = fields.get("from");
        String to = fields.get("to");

        if (usdValueStr != null && !usdValueStr.equals("null")) {
            try {
                BigDecimal usdValue = new BigDecimal(usdValueStr);
                if (usdValue.compareTo(WHALE_THRESHOLD) > 0) {
                    log.info("🐋 WHALE ALERT: ${} transaction detected! tx={}",
                            usdValue.setScale(2, java.math.RoundingMode.HALF_UP), txHash);
                }
            } catch (NumberFormatException e) {
                log.trace("Could not parse usdValue: {}", usdValueStr);
            }
        }
    }
}
