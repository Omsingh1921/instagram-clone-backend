package com.instagramclone.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.security.jwt")
public class JwtProperties {

    private String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private long accessExpirationMs = 900000;       // 15 minutes
    private long refreshExpirationMs = 604800000;   // 7 days
}
