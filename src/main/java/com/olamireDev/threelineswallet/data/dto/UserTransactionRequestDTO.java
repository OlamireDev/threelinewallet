package com.olamireDev.threelineswallet.data.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class UserTransactionRequestDTO {

    @NotBlank
    private String transactionKey;

    @NotNull
    @Min(1)
    private Long userWalletId;

    @NotNull
    private BigDecimal amount;

    @NotNull
    @Min(1)
    private Long creditedWalletId;

}
