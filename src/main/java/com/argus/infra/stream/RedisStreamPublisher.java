package com.argus.infra.stream;

import java.util.HashMap;
import java.util.Map;

import com.argus.infra.stream.dto.TransactionEvent;
import com.argus.core.constant.StreamKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisStreamPublisher implements StreamPublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public String publish(String streamKey, Map<String, String> fields) {
        RecordId recordId = redisTemplate.opsForStream()
                .add(StreamRecords.mapBacked(fields).withStreamKey(streamKey));

        log.debug("Published to {}: {}", streamKey, recordId);
        return recordId.getValue();
    }

    public String publishEvent(TransactionEvent event) {
        Map<String, Object> map = objectMapper.convertValue(event, new TypeReference<Map<String, Object>>() {
        });
        Map<String, String> stringMap = new HashMap<>();
        map.forEach((key, value) -> stringMap.put(key, value.toString()));
        return publish(StreamKeys.TRANSACTION_STREAM, stringMap);
    }

}
