package com.olamireDev.threelineswallet.service;

import com.olamireDev.threelineswallet.data.exception.AuthException;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.olamireDev.threelineswallet.constants.ApplicationConstants.USERNAME;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSessionManagementService {

    private final TokenGenerationService tokenGenerationService;

    private final Map<Long, String> sessionTokenMap = new ConcurrentHashMap<>();

    public Pair<String, Date> generateSessionToken(Long userId){
        var token = tokenGenerationService.encodeData(userId.toString(),
                Map.of(USERNAME.getValue(), userId));
        sessionTokenMap.put(userId, token.getFirst());
        return token;
    }

    public Claims decodeSessionToken(String token){
        var claims = tokenGenerationService.decodeToken(token);
        var userId = Long.parseLong(claims.getSubject());
        var sessionToken = sessionTokenMap.get(userId);
        if(sessionToken == null || !sessionToken.equals(token)){
            throw new AuthException("Invalid Session Token");
        }
        return claims;
    }

}
