package com.olamireDev.threelineswallet.config;

import com.olamireDev.threelineswallet.service.TokenGenerationService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@ExtendWith(MockitoExtension.class)
class ApplicationFilterTest {

    @Mock
    private TokenGenerationService tokenGenerationService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Claims claims;

    @InjectMocks
    private ApplicationFilter applicationFilter;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_whenAuthorizationHeaderMissing_returns401AndDoesNotContinueChain() throws Exception {
        when(request.getHeader(AUTHORIZATION)).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/v1/wallet");

        applicationFilter.doFilterInternal(request, response, filterChain);

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
        verifyNoInteractions(filterChain);
        verifyNoInteractions(tokenGenerationService);
    }

    @Test
    void doFilterInternal_whenAuthorizationHeaderBlank_returns401AndDoesNotContinueChain() throws Exception {
        when(request.getHeader(AUTHORIZATION)).thenReturn("   ");
        when(request.getRequestURI()).thenReturn("/api/v1/wallet");

        applicationFilter.doFilterInternal(request, response, filterChain);

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
        verifyNoInteractions(filterChain);
        verifyNoInteractions(tokenGenerationService);
    }

    @Test
    void doFilterInternal_whenTokenValid_setsAuthenticationAndContinuesChain() throws Exception {
        when(request.getHeader(AUTHORIZATION)).thenReturn("Bearer valid.jwt.token");
        when(request.getRequestURI()).thenReturn("/api/v1/wallet");
        when(claims.getSubject()).thenReturn("42");
        when(tokenGenerationService.decodeToken("valid.jwt.token")).thenReturn(claims);

        applicationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("42");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities()).isEmpty();

        verify(filterChain).doFilter(request, response);
        verify(response, never()).sendError(anyInt());
    }

    @Test
    void doFilterInternal_stripsBearerPrefixBeforeDecoding() throws Exception {
        when(request.getHeader(AUTHORIZATION)).thenReturn("Bearer abc.def.ghi");
        when(request.getRequestURI()).thenReturn("/api/v1/wallet");
        when(claims.getSubject()).thenReturn("7");
        when(tokenGenerationService.decodeToken("abc.def.ghi")).thenReturn(claims);

        applicationFilter.doFilterInternal(request, response, filterChain);

        verify(tokenGenerationService).decodeToken("abc.def.ghi");
    }
}