package josp.choosphd.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Component
public class JwtService {

    private final ChoosePhdProperties props;
    private final SecretKey key;

    public JwtService(ChoosePhdProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.getSecurity().getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String issue(String username, String role) {
        long ttlMs = props.getSecurity().getJwtTtlHours() * 3600L * 1000L;
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claims(Map.of("role", role))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMs))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long ttlSeconds() {
        return props.getSecurity().getJwtTtlHours() * 3600L;
    }
}
