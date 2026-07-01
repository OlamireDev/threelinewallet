package com.olamireDev.threelineswallet.config;

import com.olamireDev.threelineswallet.service.TokenGenerationService;
import com.olamireDev.threelineswallet.service.WalletService;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@RequiredArgsConstructor
@Slf4j
public class ApplicationFilter implements Filter {

    private final TokenGenerationService tokenGenerationService;

    private Set<String> excludeUrls = Set.of("/error");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        var servletRequest = (HttpServletRequest) request;
        var servletResponse = (HttpServletResponse) response;
        log.info("Filtering request {}", servletRequest.getRequestURI());
        if(excludeUrls.contains(servletRequest.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }
        var authHeader = servletRequest.getHeader(AUTHORIZATION);
        if(StringUtils.isBlank(authHeader)){
            log.error("Authorization header is empty");
            servletResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        var tokenValue = authHeader.replace("Bearer ", "");
        var claims = tokenGenerationService.decodeToken(tokenValue);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(claims.getSubject(), List.of()));
        log.info("Authentication Success {}", servletRequest.getRequestURI());
        chain.doFilter(request, response);
    }

}
