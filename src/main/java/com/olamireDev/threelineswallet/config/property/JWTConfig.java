package com.olamireDev.threelineswallet.config.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "security.jwt")
public class JWTConfig {

    private String secret;

    private String issuer;

    private int ttlMinutes;

}
