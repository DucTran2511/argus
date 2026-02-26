package com.argus.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests for AlertConditions.matches(Signal signal).
 * These tests are written BEFORE the implementation.
 */
class AlertConditionsTest {

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Build a minimal WHALE_BUY signal with a given USD value (no wallet). */
    private Signal whaleBuy(BigDecimal usdValue) {
        return Signal.builder()
                .id(1L)
                .type(SignalType.WHALE_BUY.name())
                .usdValue(usdValue)
                .chain("ethereum")
                .build();
    }

    /**
     * Build a WHALE_BUY signal that also carries a walletId (for archetype tests).
     */
    private Signal whaleBuyWithWallet(BigDecimal usdValue, java.util.UUID walletId) {
        return Signal.builder()
                .id(2L)
                .type(SignalType.WHALE_BUY.name())
                .usdValue(usdValue)
                .walletId(walletId)
                .chain("ethereum")
                .build();
    }

    private Signal signalOfType(String type, BigDecimal usdValue) {
        return Signal.builder()
                .id(3L)
                .type(type)
                .usdValue(usdValue)
                .chain("ethereum")
                .build();
    }

    // ── signalTypes filter ────────────────────────────────────────────────────

    @Test
    void matches_whenSignalTypeInList_shouldReturnTrue() {
        AlertConditions conditions = AlertConditions.builder()
                .signalTypes(List.of("WHALE_BUY"))
                .build();

        assertTrue(conditions.matches(whaleBuy(new BigDecimal("100000"))));
    }

    @Test
    void matches_whenSignalTypeNotInList_shouldReturnFalse() {
        AlertConditions conditions = AlertConditions.builder()
                .signalTypes(List.of("SNIPER_ALPHA"))
                .build();

        assertFalse(conditions.matches(whaleBuy(new BigDecimal("100000"))));
    }

    @Test
    void matches_whenSignalTypesEmpty_shouldMatchAnyType() {
        // Empty signalTypes means "all types pass" — no restriction
        AlertConditions conditions = AlertConditions.builder()
                .signalTypes(List.of())
                .build();

        assertTrue(conditions.matches(whaleBuy(new BigDecimal("100000"))));
    }

    @Test
    void matches_whenSignalTypesNull_shouldMatchAnyType() {
        AlertConditions conditions = AlertConditions.builder()
                .build(); // signalTypes defaults to null

        assertTrue(conditions.matches(whaleBuy(new BigDecimal("100000"))));
    }

    @Test
    void matches_multipleTypesInList_shouldMatchAny() {
        AlertConditions conditions = AlertConditions.builder()
                .signalTypes(List.of("WHALE_SELL", "WHALE_BUY"))
                .build();

        assertTrue(conditions.matches(signalOfType("WHALE_SELL", new BigDecimal("50000"))));
        assertTrue(conditions.matches(whaleBuy(new BigDecimal("50000"))));
    }

    // ── minAmountUsd filter ───────────────────────────────────────────────────

    @Test
    void matches_whenUsdValueAboveMin_shouldReturnTrue() {
        AlertConditions conditions = AlertConditions.builder()
                .signalTypes(List.of("WHALE_BUY"))
                .minAmountUsd(new BigDecimal("100000"))
                .build();

        assertTrue(conditions.matches(whaleBuy(new BigDecimal("150000"))));
    }

    @Test
    void matches_whenUsdValueEqualsMin_shouldReturnTrue() {
        AlertConditions conditions = AlertConditions.builder()
                .signalTypes(List.of("WHALE_BUY"))
                .minAmountUsd(new BigDecimal("100000"))
                .build();

        assertTrue(conditions.matches(whaleBuy(new BigDecimal("100000"))));
    }

    @Test
    void matches_whenUsdValueBelowMin_shouldReturnFalse() {
        AlertConditions conditions = AlertConditions.builder()
                .signalTypes(List.of("WHALE_BUY"))
                .minAmountUsd(new BigDecimal("100000"))
                .build();

        assertFalse(conditions.matches(whaleBuy(new BigDecimal("50000"))));
    }

    @Test
    void matches_whenMinAmountUsdIsNull_shouldNotFilterByAmount() {
        AlertConditions conditions = AlertConditions.builder()
                .signalTypes(List.of("WHALE_BUY"))
                .minAmountUsd(null)
                .build();

        // Very small value should still match when no min is set
        assertTrue(conditions.matches(whaleBuy(new BigDecimal("1"))));
    }

    // ── chains filter ─────────────────────────────────────────────────────────

    @Test
    void matches_whenChainInList_shouldReturnTrue() {
        AlertConditions conditions = AlertConditions.builder()
                .chains(List.of("ethereum"))
                .build();

        assertTrue(conditions.matches(whaleBuy(new BigDecimal("100000"))));
    }

    @Test
    void matches_whenChainNotInList_shouldReturnFalse() {
        AlertConditions conditions = AlertConditions.builder()
                .chains(List.of("solana"))
                .build();

        assertFalse(conditions.matches(whaleBuy(new BigDecimal("100000"))));
    }

    @Test
    void matches_whenChainsNullOrEmpty_shouldMatchAnyChain() {
        AlertConditions conditions = AlertConditions.builder()
                .build();

        assertTrue(conditions.matches(whaleBuy(new BigDecimal("100000"))));
    }

    // ── tokenAddresses filter ─────────────────────────────────────────────────

    @Test
    void matches_whenTokenAddressInList_shouldReturnTrue() {
        String token = "0xABCDEF";
        Signal signal = Signal.builder()
                .id(4L)
                .type("WHALE_BUY")
                .usdValue(new BigDecimal("100000"))
                .tokenAddress(token)
                .chain("ethereum")
                .build();

        AlertConditions conditions = AlertConditions.builder()
                .tokenAddresses(List.of(token))
                .build();

        assertTrue(conditions.matches(signal));
    }

    @Test
    void matches_whenTokenAddressNotInList_shouldReturnFalse() {
        Signal signal = Signal.builder()
                .id(5L)
                .type("WHALE_BUY")
                .usdValue(new BigDecimal("100000"))
                .tokenAddress("0xOTHER")
                .chain("ethereum")
                .build();

        AlertConditions conditions = AlertConditions.builder()
                .tokenAddresses(List.of("0xABCDEF"))
                .build();

        assertFalse(conditions.matches(signal));
    }

    @Test
    void matches_whenTokenAddressesNullOrEmpty_shouldMatchAnyToken() {
        Signal signal = Signal.builder()
                .id(6L)
                .type("WHALE_BUY")
                .usdValue(new BigDecimal("100000"))
                .tokenAddress("0xANY")
                .chain("ethereum")
                .build();

        AlertConditions conditions = AlertConditions.builder()
                .build(); // tokenAddresses null

        assertTrue(conditions.matches(signal));
    }

    // ── combined filters ──────────────────────────────────────────────────────

    @Test
    void matches_withAllConditionsSet_allMustPass() {
        String token = "0xTOKEN";
        Signal signal = Signal.builder()
                .id(7L)
                .type("WHALE_BUY")
                .usdValue(new BigDecimal("200000"))
                .tokenAddress(token)
                .chain("ethereum")
                .build();

        AlertConditions conditions = AlertConditions.builder()
                .signalTypes(List.of("WHALE_BUY"))
                .minAmountUsd(new BigDecimal("100000"))
                .tokenAddresses(List.of(token))
                .chains(List.of("ethereum"))
                .build();

        assertTrue(conditions.matches(signal));
    }

    @Test
    void matches_withAllConditionsSet_failsWhenOneConditionFails() {
        String token = "0xTOKEN";
        Signal signal = Signal.builder()
                .id(8L)
                .type("WHALE_BUY")
                .usdValue(new BigDecimal("50000")) // below minAmountUsd
                .tokenAddress(token)
                .chain("ethereum")
                .build();

        AlertConditions conditions = AlertConditions.builder()
                .signalTypes(List.of("WHALE_BUY"))
                .minAmountUsd(new BigDecimal("100000"))
                .tokenAddresses(List.of(token))
                .chains(List.of("ethereum"))
                .build();

        assertFalse(conditions.matches(signal));
    }

    // ── edge cases ────────────────────────────────────────────────────────────

    @Test
    void matches_whenSignalHasNullUsdValue_shouldNotMatchAmountFilter() {
        AlertConditions conditions = AlertConditions.builder()
                .minAmountUsd(new BigDecimal("100000"))
                .build();

        Signal signal = Signal.builder()
                .id(9L)
                .type("WHALE_BUY")
                .usdValue(null)
                .chain("ethereum")
                .build();

        assertFalse(conditions.matches(signal));
    }

    @Test
    void matches_emptyConditions_shouldMatchAnySignal() {
        // All null/empty conditions = no filters = everything matches
        AlertConditions conditions = AlertConditions.builder().build();

        Signal signal = Signal.builder()
                .id(10L)
                .type("ACCUMULATION")
                .usdValue(new BigDecimal("1"))
                .chain("bsc")
                .tokenAddress("0xRANDOM")
                .build();

        assertTrue(conditions.matches(signal));
    }
}
