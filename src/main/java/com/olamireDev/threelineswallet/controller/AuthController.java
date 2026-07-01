package com.olamireDev.threelineswallet.controller;

import com.olamireDev.threelineswallet.data.dto.ApiResponse;
import com.olamireDev.threelineswallet.data.dto.CreateUserRequestDTO;
import com.olamireDev.threelineswallet.data.dto.LoginRequestDTO;
import com.olamireDev.threelineswallet.data.dto.LoginResponseDTO;
import com.olamireDev.threelineswallet.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> register(@Valid @RequestBody CreateUserRequestDTO requestDTO) {
        return  ResponseEntity.ok(ApiResponse.success(authService.createUser(requestDTO)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> login(@Valid @RequestBody LoginRequestDTO requestDTO) {
        return  ResponseEntity.ok(ApiResponse.success(authService.login(requestDTO)));
    }

}
