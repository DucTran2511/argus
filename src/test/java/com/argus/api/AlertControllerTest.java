package com.argus.api;

import com.argus.core.exception.AlertNotFoundException;
import com.argus.core.exception.GlobalExceptionHandler;
import com.argus.core.security.UserContext;
import com.argus.domain.model.Alert;
import com.argus.domain.service.AlertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TDD controller tests for AlertController.
 * Tests are written BEFORE the implementation.
 *
 * Architecture: AlertController → AlertService → AlertPersistencePort
 * (follows the hexagonal pattern used by WalletController → WalletService)
 */
@WebMvcTest(AlertController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AlertService alertService;

    @MockBean
    private UserContext userContext;

    private final UUID TEST_USER_ID = UUID.randomUUID();
    private final UUID ALERT_RULE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(userContext.getUserId()).thenReturn(TEST_USER_ID);
    }

    // ── GET /api/v1/alerts ────────────────────────────────────────────────────

    @Test
    void listAlerts_shouldReturn200WithAlerts() throws Exception {
        Alert alert = Alert.builder()
                .id(UUID.randomUUID())
                .userId(TEST_USER_ID)
                .alertRuleId(ALERT_RULE_ID)
                .signalId(1L)
                .status("PENDING")
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(alertService.getAlerts(TEST_USER_ID, 50)).thenReturn(List.of(alert));

        mockMvc.perform(get("/api/v1/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].read").value(false));
    }

    @Test
    void listAlerts_withLimitParam_shouldPassLimitToService() throws Exception {
        when(alertService.getAlerts(eq(TEST_USER_ID), eq(10))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/alerts").param("limit", "10"))
                .andExpect(status().isOk());

        verify(alertService).getAlerts(TEST_USER_ID, 10);
    }

    @Test
    void listAlerts_whenEmpty_shouldReturn200WithEmptyList() throws Exception {
        when(alertService.getAlerts(eq(TEST_USER_ID), anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── PATCH /api/v1/alerts/{id}/read ────────────────────────────────────────

    @Test
    void markAsRead_shouldReturn204() throws Exception {
        UUID alertId = UUID.randomUUID();
        doNothing().when(alertService).markAsRead(alertId, TEST_USER_ID);

        mockMvc.perform(patch("/api/v1/alerts/{id}/read", alertId))
                .andExpect(status().isNoContent());

        verify(alertService).markAsRead(alertId, TEST_USER_ID);
    }

    @Test
    void markAsRead_whenAlertNotFound_shouldReturn404() throws Exception {
        UUID unknownId = UUID.randomUUID();
        doThrow(new AlertNotFoundException(unknownId))
                .when(alertService).markAsRead(unknownId, TEST_USER_ID);

        mockMvc.perform(patch("/api/v1/alerts/{id}/read", unknownId))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/v1/alerts/unread-count ──────────────────────────────────────

    @Test
    void getUnreadCount_shouldReturn200WithCount() throws Exception {
        when(alertService.countUnread(TEST_USER_ID)).thenReturn(7L);

        mockMvc.perform(get("/api/v1/alerts/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(7));
    }

    @Test
    void getUnreadCount_whenZero_shouldReturnCountZero() throws Exception {
        when(alertService.countUnread(TEST_USER_ID)).thenReturn(0L);

        mockMvc.perform(get("/api/v1/alerts/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));
    }
}
