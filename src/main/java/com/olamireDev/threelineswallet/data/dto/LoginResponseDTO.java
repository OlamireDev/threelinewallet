package com.olamireDev.threelineswallet.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class LoginResponseDTO {

    private String token;

    private LocalDateTime expireOn;

    private String name;

}
