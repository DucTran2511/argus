package com.argus.infra.price.adapter;

import com.argus.domain.model.TokenPrice;
import com.argus.domain.model.TokenPrice.PriceStatus;
import com.argus.domain.port.blockchain.PricePort;
import com.argus.infra.blockchain.dto.response.DexScreenerResponse;
import com.argus.infra.blockchain.dto.response.DexScreenerResponse.DexPair;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class DexScreenerPriceAdapter implements PricePort {

    private static final Set<String> TRUSTED_DEXES = Set.of(
            "uniswap_v2", "uniswap_v3", "uniswap", "sushiswap", "pancakeswap");
    private static final BigDecimal MIN_LIQUIDITY = new BigDecimal("1000");
    private static final String WETH_ADDRESS = "0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2";

    private final RestClient restClient;

    public DexScreenerPriceAdapter(
            RestClient.Builder builder,
            @Value("${dexscreener.api.base-url:https://api.dexscreener.com/latest/dex}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public Optional<TokenPrice> getTokenPrice(String tokenAddress, String chain) {
        log.debug("Fetching price for token: {} on chain: {}", tokenAddress, chain);

        try {
            DexScreenerResponse response = restClient.get()
                    .uri("/tokens/{tokenAddress}", tokenAddress)
                    .retrieve()
                    .body(DexScreenerResponse.class);

            if (response == null || response.getPairs() == null || response.getPairs().isEmpty()) {
                log.warn("No pairs found for token: {}", tokenAddress);
                return Optional.of(buildNotFoundPrice(tokenAddress));
            }

            Optional<DexPair> bestPair = response.getPairs().stream()
                    .filter(pair -> TRUSTED_DEXES.contains(pair.getDexId().toLowerCase()))
                    .filter(pair -> pair.getLiquidity() != null
                            && pair.getLiquidity().getUsd() != null
                            && BigDecimal.valueOf(pair.getLiquidity().getUsd()).compareTo(MIN_LIQUIDITY) >= 0)
                    // Sort by liquidity DESC
                    .sorted(Comparator.comparingDouble(
                            (DexPair p) -> p.getLiquidity().getUsd()).reversed())
                    // Take top pair
                    .findFirst();

            if (bestPair.isEmpty()) {
                log.warn("No trusted DEX pairs with sufficient liquidity for token: {}", tokenAddress);
                return Optional.of(buildLowLiquidityPrice(tokenAddress));
            }

            return Optional.of(convertToTokenPrice(bestPair.get(), tokenAddress));

        } catch (RestClientException e) {
            log.error("DexScreener API error for token {}: {}", tokenAddress, e.getMessage());
            return Optional.of(buildNotFoundPrice(tokenAddress));
        }
    }

    @Override
    public BigDecimal getEthPrice() {
        log.debug("Fetching ETH price");

        return getTokenPrice(WETH_ADDRESS, "ethereum")
                .filter(p -> p.getStatus() == PriceStatus.FOUND)
                .map(TokenPrice::getPriceUsd)
                .orElse(BigDecimal.ZERO);
    }

    private TokenPrice convertToTokenPrice(DexPair pair, String tokenAddress) {
        BigDecimal priceUsd = nullSafe(pair.getPriceUsd());
        BigDecimal priceNative = nullSafe(pair.getPriceNative());

        if (priceUsd.compareTo(BigDecimal.ZERO) == 0 && priceNative.compareTo(BigDecimal.ZERO) > 0) {
            if (tokenAddress.equalsIgnoreCase(WETH_ADDRESS)) {
                log.warn("WETH priceUsd is missing! Cannot calculate.");
                return TokenPrice.builder().status(PriceStatus.NOT_FOUND).build();
            }
            BigDecimal ethPrice = getEthPrice();
            if (ethPrice.compareTo(BigDecimal.ZERO) > 0) {
                priceUsd = priceNative.multiply(ethPrice);
                log.debug("Calculated priceUsd from priceNative: {} * {} = {}",
                        priceNative, ethPrice, priceUsd);
            }
        }

        return TokenPrice.builder()
                .tokenAddress(tokenAddress)
                .symbol(pair.getBaseToken() != null ? pair.getBaseToken().getSymbol() : null)
                .priceUsd(priceUsd)
                .priceNative(priceNative)
                .liquidityUsd(BigDecimal.valueOf(pair.getLiquidity().getUsd()))
                .dexId(pair.getDexId())
                .pairAddress(pair.getPairAddress())
                .fetchAt(LocalDateTime.now())
                .status(PriceStatus.FOUND)
                .build();
    }

    private TokenPrice buildNotFoundPrice(String tokenAddress) {
        return TokenPrice.builder()
                .tokenAddress(tokenAddress)
                .status(PriceStatus.NOT_FOUND)
                .fetchAt(LocalDateTime.now())
                .build();
    }

    private TokenPrice buildLowLiquidityPrice(String tokenAddress) {
        return TokenPrice.builder()
                .tokenAddress(tokenAddress)
                .status(PriceStatus.LOW_LIQUIDITY)
                .fetchAt(LocalDateTime.now())
                .build();
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
