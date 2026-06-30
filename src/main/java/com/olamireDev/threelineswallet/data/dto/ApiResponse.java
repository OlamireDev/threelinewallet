package com.olamireDev.threelineswallet.data.dto;

import com.olamireDev.threelineswallet.constants.ErrorResponseTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ApiResponse<T> {

    private boolean success;

    private T data;

    private String errorCode;

    private String errorMessage;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder().success(true).data(data).build();
    }

    public static  ApiResponse error(ErrorResponseTypes errorResponse) {
        return ApiResponse.builder()
                .success(false)
                .errorCode(errorResponse.getCode())
                .errorMessage(errorResponse.getMessage())
                .build();
    }

}
