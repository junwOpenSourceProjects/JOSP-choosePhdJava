package com.choosephd.security;

import com.choosephd.config.ChoosePhdProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtService {

    private final ChoosePhdProperties properties;
    private SecretKey key;

    public JwtService(ChoosePhdProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        String secret = properties.getJwt().getSecret();
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String role) {
        return generateToken(userId, role, null, "free");
    }

    public String generateToken(Long userId, String role, String membership) {
        return generateToken(userId, role, null, membership);
    }

    /**
     * 签发 JWT，携带账号创建时间和会员等级。
     */
    public String generateToken(Long userId, String role, java.time.LocalDateTime createdAt, String membership) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + properties.getJwt().getExpiration());
        var builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry);
        if (createdAt != null) {
            builder.claim("createdAt", createdAt.toString());
        }
        builder.claim("membership", membership);
        return builder.signWith(key).compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
