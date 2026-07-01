package com.olamireDev.threelineswallet.service;

import com.olamireDev.threelineswallet.config.property.JWTConfig;
import com.olamireDev.threelineswallet.data.exception.AuthException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenGenerationServiceTest {

    private static final String SECRET = "M2xpbmVzV2FsbGV0Cg==aLongerSecretToSatisfyHmacShaKeyRequirements";
    private static final String ISSUER = "3linesWallet";
    private static final int TTL_MINUTES = 10;

    private JWTConfig jwtConfig;
    private TokenGenerationService tokenGenerationService;

    @BeforeEach
    void setUp() {
        jwtConfig = new JWTConfig();
        jwtConfig.setSecret(SECRET);
        jwtConfig.setIssuer(ISSUER);
        jwtConfig.setTtlMinutes(TTL_MINUTES);
        tokenGenerationService = new TokenGenerationService(jwtConfig);
    }

    @Test
    void encodeData_returnsTokenAndExpiryAroundConfiguredTtl() {
        var before = Instant.now();
        var result = tokenGenerationService.encodeData("user-123", Map.of("username", "Jane"));
        var after = Instant.now();

        assertThat(result.getFirst()).isNotBlank();
        assertThat(result.getFirst().split("\\.")).hasSize(3); // header.payload.signature

        Instant expiry = result.getSecond().toInstant();
        assertThat(expiry).isAfterOrEqualTo(before.plus(TTL_MINUTES, ChronoUnit.MINUTES).minusSeconds(1));
        assertThat(expiry).isBeforeOrEqualTo(after.plus(TTL_MINUTES, ChronoUnit.MINUTES).plusSeconds(1));
    }

    @Test
    void encodeData_tokenContainsCorrectSubjectAndIssuer() {
        var result = tokenGenerationService.encodeData("user-123", Map.of("username", "Jane"));

        Claims claims = parseSignedToken(result.getFirst(), SECRET);

        assertThat(claims.getSubject()).isEqualTo("user-123");
        assertThat(claims.getIssuer()).isEqualTo(ISSUER);
    }

    @Test
    void encodeData_customClaimsArePresentInPayload() {
        var result = tokenGenerationService.encodeData("user-123", Map.of("username", "Jane", "role", "OWNER"));

        Claims claims = parseSignedToken(result.getFirst(), SECRET);

        assertThat(claims.get("username", String.class)).isEqualTo("Jane");
        assertThat(claims.get("role", String.class)).isEqualTo("OWNER");
    }

    @Test
    void encodeData_tokenFailsVerificationWithWrongKey() {
        var result = tokenGenerationService.encodeData("user-123", Map.of("username", "Jane"));
        String wrongKey = "aCompletelyDifferentSecretKeyThatIsLongEnoughForHmacSha";

        assertThatThrownBy(() -> parseSignedToken(result.getFirst(), wrongKey))
                .isInstanceOf(io.jsonwebtoken.security.SignatureException.class);
    }

    @Test
    void encodeData_zeroTtl_producesAlreadyExpiredToken() {
        jwtConfig.setTtlMinutes(0);
        tokenGenerationService = new TokenGenerationService(jwtConfig);

        var result = tokenGenerationService.encodeData("user-123", Map.of());

        assertThat(result.getSecond()).isBeforeOrEqualTo(new Date(System.currentTimeMillis() + 1000));
    }

    @Test
    void decodeToken_BUG_throwsInvalidIssuerEvenWhenIssuerMatches() {
        var result = tokenGenerationService.encodeData("user-123", Map.of("username", "Jane"));

        assertThatThrownBy(() -> invokeDecodeToken(result.getFirst()))
                .isInstanceOf(Exception.class);
    }

    @Test
    void decodeToken_shouldDecodeASignedToken_onceParserBugIsFixed() {
        var result = tokenGenerationService.encodeData("user-123", Map.of("username", "Jane"));
        // Desired behavior once fixed:
         Claims claims = tokenGenerationService.decodeToken(result.getFirst());
         assertThat(claims.getSubject()).isEqualTo("user-123");
    }


    private static Claims parseSignedToken(String token, String secret) {
        Key key = Keys.hmacShaKeyFor(secret.getBytes());
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Object invokeDecodeToken(String token) throws Exception {
        Method method = TokenGenerationService.class.getDeclaredMethod("decodeToken", String.class);
        method.setAccessible(true);
        try {
            return method.invoke(tokenGenerationService, token);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }
}