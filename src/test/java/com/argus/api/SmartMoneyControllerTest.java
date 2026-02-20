package com.argus.api;

import com.argus.api.dto.response.SmartMoneyResponse;
import com.argus.core.exception.GlobalExceptionHandler;
import com.argus.domain.model.SmartMoneyArchetype;
import com.argus.domain.model.WalletMetrics;
import com.argus.domain.port.persistence.WalletMetricsPersistencePort;
import com.argus.domain.service.SmartMoneyScoringService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SmartMoneyController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class SmartMoneyControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private WalletMetricsPersistencePort walletMetricsPort;

        @MockBean
        private SmartMoneyScoringService scoringService;

        private static final String VALID_ADDRESS = "0x1234567890123456789012345678901234567890";

        @Test
        void getAllMetrics_shouldReturnPaginatedResponse() throws Exception {
                WalletMetrics metrics = WalletMetrics.builder()
                                .walletAddress(VALID_ADDRESS)
                                .archetype(SmartMoneyArchetype.SNIPER)
                                .totalScore(new BigDecimal("95.5"))
                                .tier("S")
                                .build();

                Page<WalletMetrics> page = new PageImpl<>(List.of(metrics));
                when(walletMetricsPort.findAll(any(Pageable.class))).thenReturn(page);

                mockMvc.perform(get("/api/v1/smart-money")
                                .param("page", "0")
                                .param("size", "20"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[0].address").value(VALID_ADDRESS))
                                .andExpect(jsonPath("$.content[0].tier").value("S"));
        }

        @Test
        void getWalletMetrics_shouldReturnMetrics() throws Exception {
                WalletMetrics metrics = WalletMetrics.builder()
                                .walletAddress(VALID_ADDRESS)
                                .totalScore(new BigDecimal("95.5"))
                                .build();

                when(walletMetricsPort.findByWalletAddress(VALID_ADDRESS)).thenReturn(Optional.of(metrics));

                mockMvc.perform(get("/api/v1/smart-money/" + VALID_ADDRESS))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.address").value(VALID_ADDRESS));
        }

        @Test
        void getWalletMetrics_shouldReturn404IfNotFound() throws Exception {
                String address = "0xf000000000000000000000000000000000000999";
                when(walletMetricsPort.findByWalletAddress(address)).thenReturn(Optional.empty());

                mockMvc.perform(get("/api/v1/smart-money/" + address))
                                .andExpect(status().isNotFound());
        }

        @Test
        void getTopWallets_shouldReturnSortedList() throws Exception {
                String address = "0x1111111111111111111111111111111111111111";
                WalletMetrics top = WalletMetrics.builder()
                                .walletAddress(address)
                                .totalScore(new BigDecimal("99.0"))
                                .build();

                Page<WalletMetrics> page = new PageImpl<>(List.of(top));
                when(walletMetricsPort.findAll(any(Pageable.class))).thenReturn(page);

                mockMvc.perform(get("/api/v1/smart-money/top")
                                .param("sortBy", "total")
                                .param("limit", "10"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(1))
                                .andExpect(jsonPath("$[0].address").value(address));
        }

        @Test
        void refreshMetrics_shouldReturnRefreshedMetrics() throws Exception {
                WalletMetrics refreshed = WalletMetrics.builder()
                                .walletAddress(VALID_ADDRESS)
                                .totalScore(new BigDecimal("88.0"))
                                .build();

                when(scoringService.calculateMetrics(VALID_ADDRESS)).thenReturn(Optional.of(refreshed));

                mockMvc.perform(post("/api/v1/smart-money/" + VALID_ADDRESS + "/refresh"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalScore").value(88.0));
        }
}
