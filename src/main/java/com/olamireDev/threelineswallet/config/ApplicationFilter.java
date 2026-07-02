package com.olamireDev.threelineswallet.config;

import com.olamireDev.threelineswallet.service.UserSessionManagementService;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@RequiredArgsConstructor
@Slf4j
public class ApplicationFilter extends OncePerRequestFilter {

    private final UserSessionManagementService userSessionManagementService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        log.info("Filtering request {}", request.getRequestURI());
        var authHeader = request.getHeader(AUTHORIZATION);
        if(StringUtils.isBlank(authHeader)){
            log.error("Authorization header is empty");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        var tokenValue = authHeader.replace("Bearer ", "");
        var claims = userSessionManagementService.decodeSessionToken(tokenValue);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(claims.getSubject(),null,  List.of()));
        log.info("Authentication Success {}", request.getRequestURI());
        filterChain.doFilter(request, response);
    }

}
