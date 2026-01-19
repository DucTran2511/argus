package com.argus.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TreeMap;

public record TokenPriceRange(
        String tokenAddress,
        TreeMap<Long, BigDecimal> pricesByTimestamp) {
    public BigDecimal getPriceAtTimestamp(LocalDateTime txTime) {
        long txEpoch = txTime.toEpochSecond(ZoneOffset.UTC);
        Map.Entry<Long, BigDecimal> entry = pricesByTimestamp.floorEntry(txEpoch);
        return entry != null ? entry.getValue() : null;
    }
}
