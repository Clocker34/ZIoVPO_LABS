package ru.rkjrth.adboard.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.rkjrth.adboard.entity.User;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Access и refresh JWT: разные TTL, claims {@code typ}, для access — {@code roles}, для refresh — стандартный {@code jti}.
 */
@Component
public class JwtTokenProvider {

    public static final String CLAIM_TOKEN_TYPE = "typ";
    public static final String CLAIM_ROLES = "roles";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final SecretKey signingKey;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration-ms}") long accessExpirationMs,
            @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        if (secret.length() < 32) {
            throw new IllegalStateException("jwt.secret must be at least 32 characters for HS256");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String createAccessToken(User user) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + accessExpirationMs);
        return Jwts.builder()
                .subject(user.getUsername())
                .claim(CLAIM_TOKEN_TYPE, TYPE_ACCESS)
                .claim(CLAIM_ROLES, user.getRole().name())
                .claim("uid", user.getId())
                .issuedAt(now)
                .expiration(exp)
                .signWith(signingKey)
                .compact();
    }

    /** Refresh: {@code jti} в стандартном claim id, связываем с {@link ru.rkjrth.adboard.entity.UserSession}. */
    public String createRefreshToken(String username, String jti) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + refreshExpirationMs);
        return Jwts.builder()
                .id(jti)
                .subject(username)
                .claim("jti", jti)
                .claim(CLAIM_TOKEN_TYPE, TYPE_REFRESH)
                .issuedAt(now)
                .expiration(exp)
                .signWith(signingKey)
                .compact();
    }

    public Claims parseAndValidate(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String typ = claims.get(CLAIM_TOKEN_TYPE, String.class);
            if (typ == null || !typ.equals(expectedType)) {
                throw new JwtException("Invalid token type");
            }
            return claims;
        } catch (ExpiredJwtException e) {
            throw new JwtException("Token expired", e);
        } catch (JwtException e) {
            throw e;
        }
    }

    public Claims validateRefreshAndGetClaims(String token) {
        return parseAndValidate(token, TYPE_REFRESH);
    }
}
