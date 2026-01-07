package com.argus.infra.cache;

import com.argus.domain.port.cache.BlockTrackingPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisBlockTrackingAdapter implements BlockTrackingPort {

    private static final String BLOCK_CURSOR_KEY = "argus:block:cursor";
    private static final String WALLET_CACHE_KEY = "argus:wallets:tracked";
    private static final Duration WALLET_CACHE_TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;

    @Override
    public Optional<Long> getLastProcessedBlock() {
        try {
            String value = redisTemplate.opsForValue().get(BLOCK_CURSOR_KEY);
            if (value != null) {
                return Optional.of(Long.parseLong(value));
            }
        } catch (Exception e) {
            log.warn("Failed to get last processed block from Redis: {}", e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public void setLastProcessedBlock(long blockNumber) {
        try {
            redisTemplate.opsForValue().set(BLOCK_CURSOR_KEY, String.valueOf(blockNumber));
            log.debug("Updated block cursor to: {}", blockNumber);
        } catch (Exception e) {
            log.error("Failed to update block cursor in Redis: {}", e.getMessage());
        }
    }

    @Override
    public Set<String> getTrackedWalletAddresses() {
        try {
            Set<String> addresses = redisTemplate.opsForSet().members(WALLET_CACHE_KEY);
            if (addresses != null && !addresses.isEmpty()) {
                log.debug("Cache hit: {} tracked wallets", addresses.size());
                return addresses;
            }
        } catch (Exception e) {
            log.warn("Failed to get wallet cache from Redis: {}", e.getMessage());
        }
        return Set.of();
    }

    @Override
    public void cacheTrackedWalletAddresses(Set<String> addresses) {
        try {
            redisTemplate.execute(new SessionCallback<Object>() {
                @Override
                public Object execute(RedisOperations ops) {
                    ops.multi();
                    ops.delete(WALLET_CACHE_KEY);
                    ops.opsForSet().add(WALLET_CACHE_KEY, addresses.toArray(new String[0]));
                    ops.expire(WALLET_CACHE_KEY, WALLET_CACHE_TTL);
                    return ops.exec();
                }
            });
        } catch (Exception e) {
            log.warn("Failed to cache wallet addresses: {}", e.getMessage());
        }
    }

    @Override
    public void invalidateWalletCache() {
        try {
            redisTemplate.delete(WALLET_CACHE_KEY);
            log.debug("Invalidated wallet cache");
        } catch (Exception e) {
            log.warn("Failed to invalidate wallet cache: {}", e.getMessage());
        }
    }
}
