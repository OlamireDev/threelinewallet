package com.olamireDev.threelineswallet.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ErrorResponseTypes {

    USER_NOT_FOUND("e3lw001", "User not found");

    private final String code;
    private final String message;

}
