package com.olamireDev.threelineswallet.controller;

import com.olamireDev.threelineswallet.data.dto.ApiResponse;
import com.olamireDev.threelineswallet.data.dto.GetWalletInfoResponseDTO;
import com.olamireDev.threelineswallet.service.WalletService;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {

    public final WalletService walletService;

    @GetMapping
    public ResponseEntity<ApiResponse<GetWalletInfoResponseDTO>> getUserWallet(){
        return ResponseEntity.ok(ApiResponse.success(walletService.getUserDefaultWalletInfo()));
    }

}
