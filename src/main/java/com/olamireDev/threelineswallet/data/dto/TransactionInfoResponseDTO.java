package com.olamireDev.threelineswallet.data.dto;

import com.olamireDev.threelineswallet.constants.TransactionType;
import com.olamireDev.threelineswallet.data.model.TransactionEntity;

import java.math.BigDecimal;
import java.util.Base64;

public record TransactionInfoResponseDTO(String transactionRef, TransactionType type, BigDecimal amount) {

    public static TransactionInfoResponseDTO fromEntity(TransactionEntity entity) {
        return new TransactionInfoResponseDTO(Base64.getEncoder().encodeToString(entity.getId().toString().getBytes()),
                entity.getTransactionType(), entity.getAmount());
    }

}
