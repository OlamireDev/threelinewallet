package com.olamireDev.threelineswallet.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Currency {

    NGN("Naira"),
    USD("US Dollar"),
    GBP("British Pound");

    private final String currencyName;

}
