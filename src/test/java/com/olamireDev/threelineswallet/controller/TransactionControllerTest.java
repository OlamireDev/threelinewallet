package com.olamireDev.threelineswallet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.olamireDev.threelineswallet.constants.TransactionType;
import com.olamireDev.threelineswallet.data.dto.TransactionInfoResponseDTO;
import com.olamireDev.threelineswallet.data.dto.TransactionResponseDTO;
import com.olamireDev.threelineswallet.data.dto.UserTransactionRequestDTO;
import com.olamireDev.threelineswallet.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TransactionController transactionController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(transactionController)
                .setValidator(new LocalValidatorFactoryBean())
                .build();
    }

    @Test
    void startTransactionSession_returnsGeneratedKey() throws Exception {
        when(transactionService.createTransactionSession()).thenReturn("session-key-123");

        mockMvc.perform(get("/api/v1/transaction/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("session-key-123"));
    }

    @Test
    void doDebitTransaction_withValidBody_returnsSuccessResponse() throws Exception {
        var request = new UserTransactionRequestDTO("key-1", 10L, BigDecimal.valueOf(300), 20L);
        when(transactionService.doUserTransaction(any(UserTransactionRequestDTO.class)))
                .thenReturn(new TransactionResponseDTO(true, "NTAx"));

        mockMvc.perform(post("/api/v1/transaction/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.transactionRef").value("NTAx"));
    }

    @Test
    void doDebitTransaction_withMissingRequiredFields_returns400() throws Exception {
        var invalidJson = "{\"transactionKey\":\"\"}";

        mockMvc.perform(post("/api/v1/transaction/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTransactionHistory_returnsMappedList() throws Exception {
        var historyItem = new TransactionInfoResponseDTO("NTAx", TransactionType.DEBIT, BigDecimal.valueOf(300));
        when(transactionService.getWalletTransactionHistory(10L)).thenReturn(List.of(historyItem));

        mockMvc.perform(get("/api/v1/transaction/history/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].transactionRef").value("NTAx"))
                .andExpect(jsonPath("$.data[0].type").value("DEBIT"))
                .andExpect(jsonPath("$.data[0].amount").value(300));
    }
}