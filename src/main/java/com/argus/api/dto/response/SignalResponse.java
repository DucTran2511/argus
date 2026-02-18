package com.argus.api.dto.response;

import com.argus.domain.model.Signal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignalResponse {
    private Long id;
    private String type;
    private UUID walletId;
    private String tokenAddress;
    private String tokenSymbol;
    private String chain;
    private BigDecimal usdValue;
    private BigDecimal confidenceScore;
    private String txHash;
    private String metadata;
    private LocalDateTime createdAt;

    private String walletArchetype;
    private String walletTier;
    private BigDecimal pnlScore;

    public static SignalResponse from(Signal signal) {
        return SignalResponse.builder()
                .id(signal.getId())
                .type(signal.getType())
                .walletId(signal.getWalletId())
                .tokenAddress(signal.getTokenAddress())
                .tokenSymbol(signal.getTokenSymbol())
                .chain(signal.getChain())
                .usdValue(signal.getUsdValue())
                .confidenceScore(signal.getConfidenceScore())
                .txHash(signal.getTxHash())
                .metadata(signal.getMetadata())
                .createdAt(signal.getCreatedAt())
                .build();
    }

    public static SignalResponse from(Signal signal, com.fasterxml.jackson.databind.ObjectMapper mapper) {
        SignalResponse response = from(signal);
        if (signal.getMetadata() != null && !signal.getMetadata().isEmpty()) {
            enrichFromMetadata(response, signal.getMetadata(), mapper);
        }
        return response;
    }

    private static void enrichFromMetadata(SignalResponse response, String metadata,
            com.fasterxml.jackson.databind.ObjectMapper mapper) {
        try {
            com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(metadata);
            if (node.has("walletArchetype")) {
                response.setWalletArchetype(node.get("walletArchetype").asText());
            }
            if (node.has("walletTier")) {
                response.setWalletTier(node.get("walletTier").asText());
            }
            if (node.has("pnlScore")) {
                response.setPnlScore(new java.math.BigDecimal(node.get("pnlScore").asText()));
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException("Failed to parse signal metadata", e);
        }
    }
}
