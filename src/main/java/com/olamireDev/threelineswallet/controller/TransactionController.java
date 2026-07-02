package com.olamireDev.threelineswallet.controller;

import com.olamireDev.threelineswallet.data.dto.ApiResponse;
import com.olamireDev.threelineswallet.data.dto.TransactionInfoResponseDTO;
import com.olamireDev.threelineswallet.data.dto.TransactionResponseDTO;
import com.olamireDev.threelineswallet.data.dto.UserTransactionRequestDTO;
import com.olamireDev.threelineswallet.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transaction")
@RequiredArgsConstructor
public class TransactionController {

    public final TransactionService transactionService;

    @GetMapping("/start")
    public ResponseEntity<ApiResponse<String>> startTransactionSession(){
        return ResponseEntity.ok(ApiResponse.success(transactionService.createTransactionSession()));
    }

    @PostMapping("/debit")
    public ResponseEntity<ApiResponse<TransactionResponseDTO>> doDebitTransaction(@Valid @RequestBody UserTransactionRequestDTO requestDTO){
        return ResponseEntity.ok(ApiResponse.success(transactionService.doUserTransaction(requestDTO)));
    }

    @PostMapping("/history/{walletId}")
    public ResponseEntity<ApiResponse<List<TransactionInfoResponseDTO>>> doDebitTransaction(@PathVariable Long walletId){
        return ResponseEntity.ok(ApiResponse.success(transactionService.getWalletTransactionHistory(walletId)));
    }

}
