package com.argus.infra.price.adapter;

import com.argus.domain.model.TokenPrice;
import com.argus.domain.model.TokenPrice.PriceStatus;
import com.argus.domain.model.TokenPriceRange;
import com.argus.domain.port.blockchain.PricePort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.TreeMap;

@Slf4j
@Component
public class CoinGeckoPriceAdapter implements PricePort {

    private static final String WETH_ADDRESS = "0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2";
    private static final long RATE_LIMIT_DELAY_MS = 2000;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public CoinGeckoPriceAdapter(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${coingecko.api.base-url:https://api.coingecko.com/api/v3}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<TokenPrice> getTokenPrice(String tokenAddress, String chain) {
        log.debug("Fetching current price for token: {} on chain: {}", tokenAddress, chain);

        try {
            rateLimitDelay();

            String response = restClient.get()
                    .uri("/simple/token_price/{chain}?contract_addresses={address}&vs_currencies=usd",
                            chain, tokenAddress)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode tokenNode = root.get(tokenAddress.toLowerCase());

            if (tokenNode == null || tokenNode.get("usd") == null) {
                log.warn("No price found for token: {}", tokenAddress);
                return Optional.of(buildNotFoundPrice(tokenAddress));
            }

            BigDecimal priceUsd = new BigDecimal(tokenNode.get("usd").asText());

            return Optional.of(TokenPrice.builder()
                    .tokenAddress(tokenAddress)
                    .priceUsd(priceUsd)
                    .fetchAt(LocalDateTime.now())
                    .status(PriceStatus.FOUND)
                    .build());

        } catch (Exception e) {
            log.error("CoinGecko API error for token {}: {}", tokenAddress, e.getMessage());
            return Optional.of(buildNotFoundPrice(tokenAddress));
        }
    }

    @Override
    public BigDecimal getEthPrice() {
        log.debug("Fetching ETH price from CoinGecko");

        try {
            rateLimitDelay();

            String response = restClient.get()
                    .uri("/simple/price?ids=ethereum&vs_currencies=usd")
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode ethNode = root.get("ethereum");

            if (ethNode != null && ethNode.get("usd") != null) {
                return new BigDecimal(ethNode.get("usd").asText());
            }

            log.warn("Could not fetch ETH price from CoinGecko");
            return BigDecimal.ZERO;

        } catch (Exception e) {
            log.error("CoinGecko ETH price error: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    @Override
    public Optional<TokenPriceRange> getTokenPriceRange(String tokenAddress, LocalDateTime from, LocalDateTime to) {
        log.info("Fetching price range for token: {} from {} to {}", tokenAddress, from, to);

        try {
            rateLimitDelay();

            long fromEpoch = from.toEpochSecond(ZoneOffset.UTC);
            long toEpoch = to.toEpochSecond(ZoneOffset.UTC);

            String response = restClient.get()
                    .uri("/coins/ethereum/contract/{address}/market_chart/range?vs_currency=usd&from={from}&to={to}",
                            tokenAddress, fromEpoch, toEpoch)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode pricesNode = root.get("prices");

            if (pricesNode == null || !pricesNode.isArray() || pricesNode.isEmpty()) {
                log.warn("No price data returned for token: {}", tokenAddress);
                return Optional.empty();
            }

            TreeMap<Long, BigDecimal> pricesByTimestamp = new TreeMap<>();

            for (JsonNode pricePoint : pricesNode) {
                if (pricePoint.isArray() && pricePoint.size() >= 2) {
                    long timestampMs = pricePoint.get(0).asLong();
                    long timestampSec = timestampMs / 1000; // Convert to seconds
                    BigDecimal price = new BigDecimal(pricePoint.get(1).asText());
                    pricesByTimestamp.put(timestampSec, price);
                }
            }

            log.info("Fetched {} price points for token: {}", pricesByTimestamp.size(), tokenAddress);

            return Optional.of(new TokenPriceRange(tokenAddress, pricesByTimestamp));

        } catch (RestClientException e) {

            if (e.getMessage() != null && e.getMessage().contains("404")) {
                log.warn("Token not found on CoinGecko: {}", tokenAddress);
            } else {
                log.error("CoinGecko API error for token {}: {}", tokenAddress, e.getMessage());
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error parsing CoinGecko response for token {}: {}", tokenAddress, e.getMessage());
            return Optional.empty();
        }
    }

    private void rateLimitDelay() {
        try {
            Thread.sleep(RATE_LIMIT_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Rate limit delay interrupted");
        }
    }

    private TokenPrice buildNotFoundPrice(String tokenAddress) {
        return TokenPrice.builder()
                .tokenAddress(tokenAddress)
                .status(PriceStatus.NOT_FOUND)
                .fetchAt(LocalDateTime.now())
                .build();
    }
}
