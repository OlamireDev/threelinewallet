package com.olamireDev.threelineswallet.data.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record GetWalletInfoResponseDTO(Long walletId, BigDecimal balance, String currency) {
}
