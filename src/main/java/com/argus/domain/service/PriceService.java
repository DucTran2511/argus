package com.argus.domain.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

import com.argus.domain.model.TokenPrice;
import com.argus.domain.model.TokenPrice.PriceStatus;
import com.argus.domain.port.blockchain.PricePort;
import com.argus.domain.port.cache.CachePort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class PriceService {

    private static final String CACHE_PREFIX = "price:";
    private static final Duration CACHE_TTL = Duration.ofSeconds(60);

    private final PricePort pricePort;
    private final CachePort<String, TokenPrice> cache;

    public TokenPrice getPrice(String tokenAddress) {
        String cacheKey = CACHE_PREFIX + tokenAddress.toLowerCase();

        Optional<TokenPrice> cached = cache.get(cacheKey);
        if (cached.isPresent()) {
            log.debug("Cache hit for token: {}", tokenAddress);
            return cached.get();
        }

        log.debug("Cache miss, fetching price for: {}", tokenAddress);
        Optional<TokenPrice> fetched = pricePort.getTokenPrice(tokenAddress, "ethereum");

        TokenPrice price = fetched.orElseGet(() -> TokenPrice.builder()
                .tokenAddress(tokenAddress)
                .status(PriceStatus.NOT_FOUND)
                .build());

        cache.put(cacheKey, price, CACHE_TTL);
        log.debug("Cached price for {}: status={}", tokenAddress, price.getStatus());

        return price;
    }

    public BigDecimal calculateUsdValue(String tokenAddress, BigDecimal amount) {
        if (tokenAddress == null || amount == null) {
            return BigDecimal.ZERO;
        }

        TokenPrice price = getPrice(tokenAddress);

        if (price == null || price.getStatus() != PriceStatus.FOUND) {
            log.debug("Cannot calculate USD value: token {} has status {}",
                    tokenAddress, price != null ? price.getStatus() : "null");
            return BigDecimal.ZERO;
        }

        if (price.getPriceUsd() == null) {
            log.debug("Price found but priceUsd is null for token: {}", tokenAddress);
            return BigDecimal.ZERO;
        }

        return price.getPriceUsd().multiply(amount);
    }

    public BigDecimal getEthPrice() {
        return pricePort.getEthPrice();
    }
}
