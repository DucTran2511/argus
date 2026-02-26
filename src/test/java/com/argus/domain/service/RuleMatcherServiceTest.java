package com.argus.domain.service;

import com.argus.domain.model.Alert;
import com.argus.domain.model.AlertConditions;
import com.argus.domain.model.AlertRule;
import com.argus.domain.model.Signal;
import com.argus.domain.port.persistence.AlertPersistencePort;
import com.argus.domain.port.persistence.AlertRulePersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TDD unit tests for RuleMatcherService.
 * These tests are written BEFORE the implementation.
 */
@ExtendWith(MockitoExtension.class)
class RuleMatcherServiceTest {

    @Mock
    private AlertRulePersistencePort alertRulePort;

    @Mock
    private AlertPersistencePort alertPort;

    @InjectMocks
    private RuleMatcherService ruleMatcherService;

    private final UUID USER_ID = UUID.randomUUID();
    private final UUID RULE_ID = UUID.randomUUID();

    // A WHALE_BUY signal worth $150k
    private Signal whaleBuySignal;

    // A rule that matches WHALE_BUY >= $100k
    private AlertRule whaleBuyRule;

    @BeforeEach
    void setUp() {
        whaleBuySignal = Signal.builder()
                .id(42L)
                .type("WHALE_BUY")
                .usdValue(new BigDecimal("150000"))
                .chain("ethereum")
                .build();

        AlertConditions conditions = AlertConditions.builder()
                .signalTypes(List.of("WHALE_BUY"))
                .minAmountUsd(new BigDecimal("100000"))
                .build();

        whaleBuyRule = AlertRule.builder()
                .id(RULE_ID)
                .userId(USER_ID)
                .name("Whale Buy >100K")
                .conditions(conditions)
                .enabled(true)
                .build();
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void matchSignal_whenRuleMatches_shouldCreateAlert() {
        when(alertRulePort.findAllEnabled()).thenReturn(List.of(whaleBuyRule));
        when(alertPort.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

        ruleMatcherService.matchSignal(whaleBuySignal);

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertPort).save(captor.capture());

        Alert saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getAlertRuleId()).isEqualTo(RULE_ID);
        assertThat(saved.getSignalId()).isEqualTo(42L);
        assertThat(saved.getStatus()).isEqualTo("PENDING");
        assertThat(saved.isRead()).isFalse();
    }

    @Test
    void matchSignal_whenMultipleRulesMatch_shouldCreateMultipleAlerts() {
        UUID ruleId2 = UUID.randomUUID();
        AlertRule anotherRule = AlertRule.builder()
                .id(ruleId2)
                .userId(UUID.randomUUID())
                .name("All WHALE_BUY")
                .conditions(AlertConditions.builder()
                        .signalTypes(List.of("WHALE_BUY"))
                        .build())
                .enabled(true)
                .build();

        when(alertRulePort.findAllEnabled()).thenReturn(List.of(whaleBuyRule, anotherRule));
        when(alertPort.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

        ruleMatcherService.matchSignal(whaleBuySignal);

        verify(alertPort, times(2)).save(any(Alert.class));
    }

    // ── No match ──────────────────────────────────────────────────────────────

    @Test
    void matchSignal_whenNoRulesMatch_shouldNotCreateAlert() {
        // Signal: ACCUMULATION — rule expects WHALE_BUY => no match
        Signal accumulationSignal = Signal.builder()
                .id(99L)
                .type("ACCUMULATION")
                .usdValue(new BigDecimal("200000"))
                .chain("ethereum")
                .build();

        when(alertRulePort.findAllEnabled()).thenReturn(List.of(whaleBuyRule));

        ruleMatcherService.matchSignal(accumulationSignal);

        verify(alertPort, never()).save(any(Alert.class));
    }

    @Test
    void matchSignal_whenSignalBelowMinAmount_shouldNotCreateAlert() {
        Signal smallSignal = Signal.builder()
                .id(88L)
                .type("WHALE_BUY")
                .usdValue(new BigDecimal("50000")) // rule needs >= 100k
                .chain("ethereum")
                .build();

        when(alertRulePort.findAllEnabled()).thenReturn(List.of(whaleBuyRule));

        ruleMatcherService.matchSignal(smallSignal);

        verify(alertPort, never()).save(any(Alert.class));
    }

    // ── No enabled rules ──────────────────────────────────────────────────────

    @Test
    void matchSignal_whenNoEnabledRules_shouldDoNothing() {
        when(alertRulePort.findAllEnabled()).thenReturn(List.of());

        ruleMatcherService.matchSignal(whaleBuySignal);

        verify(alertPort, never()).save(any(Alert.class));
    }

    // ── Null signal guard ─────────────────────────────────────────────────────

    @Test
    void matchSignal_whenSignalIsNull_shouldDoNothingGracefully() {
        // Should not throw, should not call port methods
        ruleMatcherService.matchSignal(null);

        verify(alertRulePort, never()).findAllEnabled();
        verify(alertPort, never()).save(any(Alert.class));
    }

    // ── Disabled rule guard ───────────────────────────────────────────────────

    @Test
    void matchSignal_skipsDisabledRules_byLoadingOnlyEnabled() {
        // findAllEnabled() is the contract — disabled rules should never reach
        // matchSignal.
        // Here we verify the service delegates rule loading to findAllEnabled, not
        // findAll.
        when(alertRulePort.findAllEnabled()).thenReturn(List.of());

        ruleMatcherService.matchSignal(whaleBuySignal);

        verify(alertRulePort).findAllEnabled();
        verifyNoMoreInteractions(alertRulePort);
    }

    // ── Alert status ──────────────────────────────────────────────────────────

    @Test
    void matchSignal_createdAlert_shouldHavePendingStatusAndUnread() {
        when(alertRulePort.findAllEnabled()).thenReturn(List.of(whaleBuyRule));
        when(alertPort.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

        ruleMatcherService.matchSignal(whaleBuySignal);

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertPort).save(captor.capture());

        Alert alert = captor.getValue();
        assertThat(alert.getStatus()).isEqualTo("PENDING");
        assertThat(alert.isRead()).isFalse();
    }
}
