package com.argus.domain.port.cache;

import java.time.Duration;
import java.util.Optional;

public interface CachePort<K, V> {
    Optional<V> get(K key);

    void put(K key, V value, Duration ttl);
}
