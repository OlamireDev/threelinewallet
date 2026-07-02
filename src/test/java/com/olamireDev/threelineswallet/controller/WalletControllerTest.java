package com.olamireDev.threelineswallet.controller;

import com.olamireDev.threelineswallet.data.dto.GetWalletInfoResponseDTO;
import com.olamireDev.threelineswallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

    @Mock
    private WalletService walletService;

    @InjectMocks
    private WalletController walletController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(walletController).build();
    }

    @Test
    void getUserWallet_returnsOkWithWalletInfo() throws Exception {
        var dto = GetWalletInfoResponseDTO.builder()
                .walletId(1L)
                .balance(BigDecimal.valueOf(1500))
                .currency("Naira")
                .build();
        when(walletService.getUserDefaultWalletInfo()).thenReturn(dto);

        mockMvc.perform(get("/api/v1/wallet"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.walletId").value(1))
                .andExpect(jsonPath("$.data.balance").value(1500))
                .andExpect(jsonPath("$.data.currency").value("Naira"));
    }

    @Test
    void getUserWallet_whenServiceThrows_propagatesAsServerError() throws Exception {
        when(walletService.getUserDefaultWalletInfo())
                .thenThrow(new RuntimeException("No NGN wallet for user with id 5"));

        mockMvc.perform(get("/api/v1/wallet"))
                .andExpect(status().is5xxServerError());
    }
}