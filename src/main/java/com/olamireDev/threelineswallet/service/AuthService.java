package com.olamireDev.threelineswallet.service;

import com.olamireDev.threelineswallet.data.dto.CreateUserRequestDTO;
import com.olamireDev.threelineswallet.data.dto.LoginRequestDTO;
import com.olamireDev.threelineswallet.data.dto.LoginResponseDTO;
import com.olamireDev.threelineswallet.data.exception.AuthException;
import com.olamireDev.threelineswallet.data.exception.NotFoundException;
import com.olamireDev.threelineswallet.data.model.UserEntity;
import com.olamireDev.threelineswallet.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.olamireDev.threelineswallet.constants.ApplicationConstants.USERNAME;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final PasswordEncoder passwordEncoder;

    private final TokenGenerationService tokenGenerationService;

    private final WalletService walletService;

    private final UserRepository userRepo;

    public LoginResponseDTO createUser(CreateUserRequestDTO requestDTO) {
        try{
            log.info("Onboarding user: {}", requestDTO.getEmail());
            if(userRepo.existsByEmail(requestDTO.getEmail())){
                log.error("User with email {} already exists", requestDTO.getEmail());
                throw new AuthException("User with email already exists");
            }
            var encodedPassword = passwordEncoder.encode(requestDTO.getPassword());
            var newUser = UserEntity.builder()
                    .email(requestDTO.getEmail())
                    .password(encodedPassword)
                    .name(requestDTO.getName())
                    .build();
            newUser = userRepo.save(newUser);

            var tokenPair = tokenGenerationService.encodeData(newUser.getId().toString(),
                    Map.of(USERNAME.getValue(), newUser.getName()));

            UserEntity finalNewUser = newUser;
            CompletableFuture.runAsync(() ->
                    walletService.createDefaultWalletForUser(finalNewUser));

            return LoginResponseDTO.builder()
                    .token(tokenPair.getFirst())
                    .expireOn(LocalDateTime.ofInstant(tokenPair.getSecond().toInstant(), ZoneId.systemDefault()))
                    .name(newUser.getName())
                    .build();
        }catch (Exception e){
            log.error("An error occurred while creating user: {} {}",requestDTO.getEmail(), e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public LoginResponseDTO login(LoginRequestDTO requestDTO) {
        try{
            log.info("Login user: {}", requestDTO.email());
            var userOpt = userRepo.findByEmail(requestDTO.email());
            if(userOpt.isEmpty()){
                log.error("User {} not found", requestDTO.email());
                throw new NotFoundException(String.format("User %s not found", requestDTO.email()));
            }
            var user = userOpt.get();
            if(!passwordEncoder.matches(requestDTO.password(), user.getPassword())) {
                log.error("Invalid password for user {}", requestDTO.email());
                throw new NotFoundException(String.format("User %s not found", requestDTO.email()));
            }

            var tokenPair = tokenGenerationService.encodeData(user.getId().toString(),
                    Map.of(USERNAME.getValue(), user.getName()));

            return LoginResponseDTO.builder()
                    .token(tokenPair.getFirst())
                    .expireOn(LocalDateTime.ofInstant(tokenPair.getSecond().toInstant(), ZoneId.systemDefault()))
                    .name(user.getName())
                    .build();
        }catch (Exception e){
            log.error("An error occurred while logging in user {} : {}", requestDTO.email(), e.getMessage(), e);
            throw e;
        }
    }



}
