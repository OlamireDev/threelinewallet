package com.olamireDev.threelineswallet.service;

import com.olamireDev.threelineswallet.data.dto.CreateUserRequestDTO;
import com.olamireDev.threelineswallet.data.dto.LoginRequestDTO;
import com.olamireDev.threelineswallet.data.dto.LoginResponseDTO;
import com.olamireDev.threelineswallet.data.exception.AuthException;
import com.olamireDev.threelineswallet.data.exception.NotFoundException;
import com.olamireDev.threelineswallet.data.model.UserEntity;
import com.olamireDev.threelineswallet.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Pair;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    void createUser_persistsHashedPassword_notRawPassword() {
        var request = new CreateUserRequestDTO("jane@example.com", RAW_PASSWORD, "Jane Doe");
        when(userRepo.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(HASHED_PASSWORD);
        when(userRepo.save(any(UserEntity.class))).thenAnswer(inv -> {
            UserEntity passed = inv.getArgument(0);
            passed.setId(99L);
            return passed;
        });
        when(tokenGenerationService.encodeData(anyString(), anyMap()))
                .thenReturn(Pair.of("token", new Date()));

        authService.createUser(request);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepo).save(captor.capture());
        assertThat(captor.getValue().getPassword())
                .isEqualTo(HASHED_PASSWORD)
                .isNotEqualTo(RAW_PASSWORD);
    }


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
    void createUser_passesCorrectSubjectAndClaimsToTokenService() {
        var request = new CreateUserRequestDTO("jane@example.com", RAW_PASSWORD, "Jane Doe");
        when(userRepo.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn(HASHED_PASSWORD);
        when(userRepo.save(any(UserEntity.class))).thenAnswer(inv -> {
            UserEntity u = inv.getArgument(0);
            u.setId(7L);
            return u;
        });
        when(tokenGenerationService.encodeData(anyString(), anyMap()))
                .thenReturn(Pair.of("tok", new Date()));

        authService.createUser(request);

        verify(tokenGenerationService).encodeData(
                eq("7"),
                eq(Map.of("username", "Jane Doe"))
        );
    }

    @Test
    void login_happyPath_returnsToken() {
        var request = new LoginRequestDTO("jane@example.com", RAW_PASSWORD);
        UserEntity existing = UserEntity.builder()
                .id(1L)
                .email("jane@example.com")
                .password(HASHED_PASSWORD)
                .name("Jane Doe")
                .build();

        when(userRepo.findByEmail("jane@example.com")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(true);

        Date expiry = Date.from(Instant.now().plus(10, ChronoUnit.MINUTES));
        when(tokenGenerationService.encodeData(eq("1"), anyMap()))
                .thenReturn(Pair.of("signed.jwt.token", expiry));

        LoginResponseDTO response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("signed.jwt.token");
        assertThat(response.getName()).isEqualTo("Jane Doe");
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

    @Test
    void login_calledTwice_returnsDistinctTokensAndExpiries() {
        var request = new LoginRequestDTO("jane@example.com", RAW_PASSWORD);
        UserEntity existing = UserEntity.builder()
                .id(1L).email("jane@example.com").password(HASHED_PASSWORD).name("Jane Doe").build();

        when(userRepo.findByEmail("jane@example.com")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(true);

        Date firstExpiry = Date.from(Instant.now().plus(10, ChronoUnit.MINUTES));
        Date secondExpiry = Date.from(firstExpiry.toInstant().plusSeconds(5));
        when(tokenGenerationService.encodeData(eq("1"), anyMap()))
                .thenReturn(Pair.of("token-1", firstExpiry))
                .thenReturn(Pair.of("token-2", secondExpiry));

        LoginResponseDTO first = authService.login(request);
        LoginResponseDTO second = authService.login(request);

        assertThat(first.getToken()).isNotEqualTo(second.getToken());
        assertThat(second.getExpireOn()).isAfter(first.getExpireOn());
    }
}