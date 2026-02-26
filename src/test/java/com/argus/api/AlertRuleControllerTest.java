package com.argus.api;

import com.argus.api.dto.request.AlertRuleRequest;
import com.argus.api.dto.response.AlertResponse;
import com.argus.api.dto.response.AlertRuleResponse;
import com.argus.core.exception.AlertRuleNotFoundException;
import com.argus.core.exception.GlobalExceptionHandler;
import com.argus.core.security.UserContext;
import com.argus.domain.model.Alert;
import com.argus.domain.model.AlertConditions;
import com.argus.domain.model.AlertRule;
import com.argus.domain.service.AlertRuleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TDD controller tests for AlertRuleController.
 * Tests are written BEFORE the implementation.
 */
@WebMvcTest(AlertRuleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AlertRuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AlertRuleService alertRuleService;

    @MockBean
    private UserContext userContext;

    private final UUID TEST_USER_ID = UUID.randomUUID();
    private final UUID RULE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(userContext.getUserId()).thenReturn(TEST_USER_ID);
    }

    // ── POST /api/v1/alert-rules ──────────────────────────────────────────────

    @Test
    void createRule_shouldReturn201WithCreatedRule() throws Exception {
        AlertRuleRequest request = AlertRuleRequest.builder()
                .name("Whale Buys >100K")
                .conditions(AlertConditions.builder()
                        .signalTypes(List.of("WHALE_BUY"))
                        .minAmountUsd(new BigDecimal("100000"))
                        .build())
                .channels(List.of("in_app"))
                .enabled(true)
                .build();

        AlertRule created = AlertRule.builder()
                .id(RULE_ID)
                .userId(TEST_USER_ID)
                .name(request.getName())
                .conditions(request.getConditions())
                .channels(request.getChannels())
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(alertRuleService.createRule(any(AlertRule.class), eq(TEST_USER_ID))).thenReturn(created);

        mockMvc.perform(post("/api/v1/alert-rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(RULE_ID.toString()))
                .andExpect(jsonPath("$.name").value("Whale Buys >100K"))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void createRule_withBlankName_shouldReturn400() throws Exception {
        AlertRuleRequest request = AlertRuleRequest.builder()
                .name("") // blank — invalid
                .conditions(AlertConditions.builder().build())
                .enabled(true)
                .build();

        mockMvc.perform(post("/api/v1/alert-rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRule_withNullConditions_shouldReturn400() throws Exception {
        AlertRuleRequest request = AlertRuleRequest.builder()
                .name("Valid Name")
                .conditions(null) // null — invalid
                .enabled(true)
                .build();

        mockMvc.perform(post("/api/v1/alert-rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/v1/alert-rules ───────────────────────────────────────────────

    @Test
    void getRules_shouldReturn200WithList() throws Exception {
        AlertRule rule = AlertRule.builder()
                .id(RULE_ID)
                .userId(TEST_USER_ID)
                .name("My Rule")
                .conditions(AlertConditions.builder().build())
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(alertRuleService.getRules(TEST_USER_ID)).thenReturn(List.of(rule));

        mockMvc.perform(get("/api/v1/alert-rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(RULE_ID.toString()))
                .andExpect(jsonPath("$[0].name").value("My Rule"));
    }

    @Test
    void getRules_whenEmpty_shouldReturn200WithEmptyList() throws Exception {
        when(alertRuleService.getRules(TEST_USER_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/alert-rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── GET /api/v1/alert-rules/{id} ──────────────────────────────────────────

    @Test
    void getRule_shouldReturn200WithRule() throws Exception {
        AlertRule rule = AlertRule.builder()
                .id(RULE_ID)
                .userId(TEST_USER_ID)
                .name("My Rule")
                .conditions(AlertConditions.builder().build())
                .enabled(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(alertRuleService.getRule(RULE_ID, TEST_USER_ID)).thenReturn(rule);

        mockMvc.perform(get("/api/v1/alert-rules/{id}", RULE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(RULE_ID.toString()))
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void getRule_whenNotFound_shouldReturn404() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(alertRuleService.getRule(unknownId, TEST_USER_ID))
                .thenThrow(new AlertRuleNotFoundException(unknownId));

        mockMvc.perform(get("/api/v1/alert-rules/{id}", unknownId))
                .andExpect(status().isNotFound());
    }

    // ── PUT /api/v1/alert-rules/{id} ──────────────────────────────────────────

    @Test
    void updateRule_shouldReturn200WithUpdatedRule() throws Exception {
        AlertRuleRequest request = AlertRuleRequest.builder()
                .name("Updated Name")
                .conditions(AlertConditions.builder()
                        .signalTypes(List.of("WHALE_SELL"))
                        .build())
                .channels(List.of("in_app"))
                .enabled(false)
                .build();

        AlertRule updated = AlertRule.builder()
                .id(RULE_ID)
                .userId(TEST_USER_ID)
                .name("Updated Name")
                .conditions(request.getConditions())
                .enabled(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(alertRuleService.updateRule(eq(RULE_ID), any(AlertRule.class), eq(TEST_USER_ID)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/v1/alert-rules/{id}", RULE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void updateRule_whenNotFound_shouldReturn404() throws Exception {
        UUID unknownId = UUID.randomUUID();
        AlertRuleRequest request = AlertRuleRequest.builder()
                .name("Name")
                .conditions(AlertConditions.builder().build())
                .enabled(true)
                .build();

        when(alertRuleService.updateRule(eq(unknownId), any(AlertRule.class), eq(TEST_USER_ID)))
                .thenThrow(new AlertRuleNotFoundException(unknownId));

        mockMvc.perform(put("/api/v1/alert-rules/{id}", unknownId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /api/v1/alert-rules/{id} ──────────────────────────────────────

    @Test
    void deleteRule_shouldReturn204() throws Exception {
        doNothing().when(alertRuleService).deleteRule(RULE_ID, TEST_USER_ID);

        mockMvc.perform(delete("/api/v1/alert-rules/{id}", RULE_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteRule_whenNotFound_shouldReturn404() throws Exception {
        UUID unknownId = UUID.randomUUID();
        doThrow(new AlertRuleNotFoundException(unknownId))
                .when(alertRuleService).deleteRule(unknownId, TEST_USER_ID);

        mockMvc.perform(delete("/api/v1/alert-rules/{id}", unknownId))
                .andExpect(status().isNotFound());
    }
}
