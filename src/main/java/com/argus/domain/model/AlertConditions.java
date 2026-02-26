package com.argus.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Typed representation of the {@code conditions} JSONB column in
 * {@code alert_rules}.
 * <p>
 * Each non-null / non-empty field acts as an AND filter. A null or empty field
 * means "no restriction" (match everything for this dimension).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertConditions {

    /**
     * Signal types that trigger this rule, e.g. ["WHALE_BUY", "SNIPER_ALPHA"].
     * Null/empty = any type.
     */
    private List<String> signalTypes;

    /** Minimum USD value of the signal. Null = no minimum. */
    private BigDecimal minAmountUsd;

    /**
     * Wallet archetypes to filter on (reserved for future use). Null/empty = any
     * archetype.
     */
    private List<String> walletArchetypes;

    /** Specific token addresses to watch. Null/empty = any token. */
    private List<String> tokenAddresses;

    /** Specific chains to watch, e.g. ["ethereum"]. Null/empty = any chain. */
    private List<String> chains;

    /**
     * Returns {@code true} when {@code signal} satisfies every non-null condition.
     * All conditions are AND-combined.
     */
    public boolean matches(Signal signal) {
        if (signal == null) {
            return false;
        }

        // signalTypes filter
        if (signalTypes != null && !signalTypes.isEmpty()) {
            if (!signalTypes.contains(signal.getType())) {
                return false;
            }
        }

        // minAmountUsd filter
        if (minAmountUsd != null) {
            if (signal.getUsdValue() == null) {
                return false;
            }
            if (signal.getUsdValue().compareTo(minAmountUsd) < 0) {
                return false;
            }
        }

        // chains filter
        if (chains != null && !chains.isEmpty()) {
            String signalChain = signal.getChain() != null ? signal.getChain().toLowerCase() : null;
            boolean chainMatched = chains.stream()
                    .anyMatch(c -> c != null && c.toLowerCase().equals(signalChain));
            if (!chainMatched) {
                return false;
            }
        }

        // tokenAddresses filter
        if (tokenAddresses != null && !tokenAddresses.isEmpty()) {
            String signalToken = signal.getTokenAddress() != null ? signal.getTokenAddress().toLowerCase() : null;
            boolean tokenMatched = tokenAddresses.stream()
                    .anyMatch(t -> t != null && t.toLowerCase().equals(signalToken));
            if (!tokenMatched) {
                return false;
            }
        }

        return true;
    }
}
