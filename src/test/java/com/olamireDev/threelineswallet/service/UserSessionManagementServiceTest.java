package com.olamireDev.threelineswallet.service;

import com.olamireDev.threelineswallet.data.exception.AuthException;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Pair;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class UserSessionManagementServiceTest {

    @Mock
    private TokenGenerationService tokenGenerationService;

    @InjectMocks
    private UserSessionManagementService userSessionManagementService;

    @Test
    void generateSessionToken_delegatesToTokenGenerationServiceAndReturnsPair() {
        Long userId = 42L;
        Date expiry = new Date();
        when(tokenGenerationService.encodeData(eq("42"), anyMap()))
                .thenReturn(Pair.of("signed.jwt.token", expiry));

        Pair<String, Date> result = userSessionManagementService.generateSessionToken(userId);

        assertThat(result.getFirst()).isEqualTo("signed.jwt.token");
        assertThat(result.getSecond()).isEqualTo(expiry);
        verify(tokenGenerationService).encodeData(eq("42"), anyMap());
    }

    @Test
    void decodeSessionToken_whenTokenMatchesActiveSession_returnsClaims() {
        Long userId = 7L;
        when(tokenGenerationService.encodeData(eq("7"), anyMap()))
                .thenReturn(Pair.of("session.token.value", new Date()));
        userSessionManagementService.generateSessionToken(userId);

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("7");
        when(tokenGenerationService.decodeToken("session.token.value")).thenReturn(claims);

        Claims result = userSessionManagementService.decodeSessionToken("session.token.value");

        assertThat(result.getSubject()).isEqualTo("7");
    }

    @Test
    void decodeSessionToken_whenNoSessionEverIssuedForUser_throwsAuthException() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("99");
        when(tokenGenerationService.decodeToken("orphaned.token")).thenReturn(claims);

        assertThatThrownBy(() -> userSessionManagementService.decodeSessionToken("orphaned.token"))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void decodeSessionToken_whenTokenDoesNotMatchStoredSessionToken_throwsAuthException() {
        Long userId = 8L;
        when(tokenGenerationService.encodeData(eq("8"), anyMap()))
                .thenReturn(Pair.of("original.session.token", new Date()));
        userSessionManagementService.generateSessionToken(userId);

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("8");
        when(tokenGenerationService.decodeToken("different.token")).thenReturn(claims);

        assertThatThrownBy(() -> userSessionManagementService.decodeSessionToken("different.token"))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void decodeSessionToken_whenNewSessionIssuedForSameUser_invalidatesPreviousToken() {
        Long userId = 15L;
        when(tokenGenerationService.encodeData(eq("15"), anyMap()))
                .thenReturn(Pair.of("first.token", new Date()))
                .thenReturn(Pair.of("second.token", new Date()));

        userSessionManagementService.generateSessionToken(userId);
        userSessionManagementService.generateSessionToken(userId);

        Claims firstClaims = mock(Claims.class);
        when(firstClaims.getSubject()).thenReturn("15");
        when(tokenGenerationService.decodeToken("first.token")).thenReturn(firstClaims);

        assertThatThrownBy(() -> userSessionManagementService.decodeSessionToken("first.token"))
                .isInstanceOf(AuthException.class);
    }
}