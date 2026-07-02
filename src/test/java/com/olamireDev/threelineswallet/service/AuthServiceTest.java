package com.olamireDev.threelineswallet.service;

import com.olamireDev.threelineswallet.data.dto.CreateUserRequestDTO;
import com.olamireDev.threelineswallet.data.dto.LoginRequestDTO;
import com.olamireDev.threelineswallet.data.exception.AuthException;
import com.olamireDev.threelineswallet.data.exception.NotFoundException;
import com.olamireDev.threelineswallet.data.model.UserEntity;
import com.olamireDev.threelineswallet.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenGenerationService tokenGenerationService;

    @Mock
    private WalletService walletService;

    @Mock
    private UserRepository userRepo;

    @InjectMocks
    private AuthService authService;

    private static final String RAW_PASSWORD = "P@ssw0rd!";
    private static final String HASHED_PASSWORD = "$2a$10$hashedvaluehashedvaluehashedva";




    @Test
    void createUser_duplicateEmail_throwsAuthException() {
        var request = new CreateUserRequestDTO("dup@example.com", RAW_PASSWORD, "Someone");
        when(userRepo.existsByEmail("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.createUser(request))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("already exists");

        verify(userRepo, never()).save(any());
        verifyNoInteractions(tokenGenerationService);
    }



    @Test
    void login_unknownEmail_throwsNotFoundException() {
        var request = new LoginRequestDTO("ghost@example.com", RAW_PASSWORD);
        when(userRepo.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("ghost@example.com");

        verifyNoInteractions(tokenGenerationService);
    }

    @Test
    void login_wrongPassword_throwsNotFoundExceptionWithSameMessageAsUnknownEmail() {
        var request = new LoginRequestDTO("jane@example.com", "wrong-password");
        UserEntity existing = UserEntity.builder()
                .id(1L).email("jane@example.com").password(HASHED_PASSWORD).name("Jane Doe").build();

        when(userRepo.findByEmail("jane@example.com")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("wrong-password", HASHED_PASSWORD)).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User jane@example.com not found");

        verifyNoInteractions(tokenGenerationService);
    }

}