package com.olamireDev.threelineswallet.service;
import com.olamireDev.threelineswallet.config.property.JWTConfig;
import com.olamireDev.threelineswallet.data.exception.AuthException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenGenerationService {

    private final JWTConfig jwtConfig;

    public Pair<String, Date> encodeData(String subject, Map<String, Object> claims) {
        var key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes());

        var expiration = Date.from(Instant.now().plus(jwtConfig.getTtlMinutes(), ChronoUnit.MINUTES));
        return Pair.of(Jwts.builder()
                .subject(subject)
                .issuer(jwtConfig.getIssuer())
                .claims(claims)
                .signWith(key)
                .expiration(expiration)
                .compact(),
                expiration);
    }

    private Claims decodeToken(String token){
        log.info("Token: {}", token);
        var key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes());
        var claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseEncryptedClaims(token)
                .getPayload();
        log.info("Claims: {}", claims);
        if(claims.getIssuer().equalsIgnoreCase(jwtConfig.getIssuer())) {
            log.error("Invalid issuer: {}", claims.getIssuer());
            throw new AuthException("Invalid Issuer");
        }
        if(claims.getExpiration().before(new Date())){
            log.error("Provided token has expired");
            throw new AuthException("Expired token");
        }
        return claims;
    }


}
