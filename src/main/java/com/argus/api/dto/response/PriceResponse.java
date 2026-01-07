package com.argus.api.dto.response;

import com.argus.domain.model.TokenPrice;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PriceResponse(
        String tokenAddress,
        String symbol,
        BigDecimal priceUsd,
        BigDecimal liquidityUsd,
        String dexId,
        String status) {
    public static PriceResponse from(TokenPrice tokenPrice) {
        return new PriceResponse(
                tokenPrice.getTokenAddress(),
                tokenPrice.getSymbol(),
                tokenPrice.getPriceUsd(),
                tokenPrice.getLiquidityUsd(),
                tokenPrice.getDexId(),
                tokenPrice.getStatus() != null ? tokenPrice.getStatus().name() : null);
    }
}
