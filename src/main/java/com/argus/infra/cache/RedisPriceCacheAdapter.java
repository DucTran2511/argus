package com.argus.infra.cache;

import com.argus.domain.model.TokenPrice;
import com.argus.domain.port.cache.CachePort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisPriceCacheAdapter implements CachePort<String, TokenPrice> {

    private final RedisTemplate<String, TokenPrice> redisTemplate;

    @Override
    public Optional<TokenPrice> get(String key) {
        try {
            TokenPrice value = redisTemplate.opsForValue().get(key);
            return Optional.ofNullable(value);
        } catch (Exception e) {
            log.warn("Redis get failed for key {}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void put(String key, TokenPrice value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
            log.debug("Cached {} with TTL {}s", key, ttl.getSeconds());
        } catch (Exception e) {
            log.warn("Redis put failed for key {}: {}", key, e.getMessage());
            // Don't throw - cache failures shouldn't break the app
        }
    }
}
