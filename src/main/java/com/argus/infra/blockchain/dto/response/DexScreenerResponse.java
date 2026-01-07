package com.argus.infra.blockchain.dto.response;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DexScreenerResponse {
    private List<DexPair> pairs;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DexPair {
        private String pairAddress;
        private String dexId;
        private BigDecimal priceUsd;
        private BigDecimal priceNative;
        private Liquidity liquidity;
        private Token baseToken;
        private Token quoteToken;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Liquidity {
        private Double usd;
        private Double base;
        private Double quote;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Token {
        private String address;
        private String name;
        private String symbol;
    }
}
