package com.argus.api;

import com.argus.api.dto.WalletRequest;
import com.argus.api.dto.WalletResponse;
import com.argus.domain.model.Wallet;
import com.argus.domain.service.WalletService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletController.class)
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WalletService walletService;

    @Test
    void createWallet_shouldReturn201() throws Exception {
        // Given
        WalletRequest request = WalletRequest.builder()
                .address("0x1234567890123456789012345678901234567890")
                .chain("ethereum")
                .label("Test Whale")
                .type("WHALE")
                .totalPnl(new BigDecimal("1000000"))
                .winRate(new BigDecimal("0.75"))
                .build();

        Wallet createdWallet = Wallet.builder()
                .id(UUID.randomUUID())
                .address(request.getAddress())
                .chain(request.getChain())
                .label(request.getLabel())
                .type(Wallet.WalletType.WHALE)
                .totalPnl(request.getTotalPnl())
                .winRate(request.getWinRate())
                .firstSeenAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(walletService.createWallet(any(Wallet.class))).thenReturn(createdWallet);

        // When & Then
        mockMvc.perform(post("/api/v1/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.address").value(request.getAddress()))
                .andExpect(jsonPath("$.chain").value(request.getChain()))
                .andExpect(jsonPath("$.label").value(request.getLabel()))
                .andExpect(jsonPath("$.type").value("WHALE"));
    }

    @Test
    void createWallet_withInvalidAddress_shouldReturn400() throws Exception {
        // Given
        WalletRequest request = WalletRequest.builder()
                .address("invalid-address")
                .chain("ethereum")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getWalletById_shouldReturn200() throws Exception {
        // Given
        UUID walletId = UUID.randomUUID();
        Wallet wallet = Wallet.builder()
                .id(walletId)
                .address("0x1234567890123456789012345678901234567890")
                .chain("ethereum")
                .label("Test Wallet")
                .type(Wallet.WalletType.WHALE)
                .build();

        when(walletService.getWalletById(walletId)).thenReturn(wallet);

        // When & Then
        mockMvc.perform(get("/api/v1/wallets/{id}", walletId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(walletId.toString()))
                .andExpect(jsonPath("$.address").value(wallet.getAddress()));
    }

    @Test
    void getAllWallets_shouldReturn200() throws Exception {
        // Given
        List<Wallet> wallets = Arrays.asList(
                Wallet.builder()
                        .id(UUID.randomUUID())
                        .address("0x1111111111111111111111111111111111111111")
                        .chain("ethereum")
                        .type(Wallet.WalletType.WHALE)
                        .build(),
                Wallet.builder()
                        .id(UUID.randomUUID())
                        .address("0x2222222222222222222222222222222222222222")
                        .chain("ethereum")
                        .type(Wallet.WalletType.VC)
                        .build());

        when(walletService.getAllWallets()).thenReturn(wallets);

        // When & Then
        mockMvc.perform(get("/api/v1/wallets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void deleteWallet_shouldReturn204() throws Exception {
        // Given
        UUID walletId = UUID.randomUUID();

        // When & Then
        mockMvc.perform(delete("/api/v1/wallets/{id}", walletId))
                .andExpect(status().isNoContent());
    }
}
