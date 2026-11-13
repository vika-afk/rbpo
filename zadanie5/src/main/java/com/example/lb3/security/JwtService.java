package com.example.lb3.security;

import com.example.lb3.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtService {

    private static final String TYPE_CLAIM = "type";
    private static final String ACCESS_TYPE = "access";
    private static final String REFRESH_TYPE = "refresh";

    private final SecretKey key;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.access-ttl-seconds}") long accessTtlSeconds,
                      @Value("${app.jwt.refresh-ttl-seconds}") long refreshTtlSeconds) {
        if (accessTtlSeconds <= 0 || refreshTtlSeconds <= 0) {
            throw new IllegalArgumentException("Срок жизни токена должен быть положительным");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtlSeconds = accessTtlSeconds;
        this.refreshTtlSeconds = refreshTtlSeconds;
    }

    public IssuedToken createAccessToken(String username) {
        return createToken(username, ACCESS_TYPE, accessTtlSeconds);
    }

    public IssuedToken createRefreshToken(String username) {
        return createToken(username, REFRESH_TYPE, refreshTtlSeconds);
    }

    public Claims validateAccessToken(String token) {
        return validateToken(token, ACCESS_TYPE);
    }

    public Claims validateRefreshToken(String token) {
        return validateToken(token, REFRESH_TYPE);
    }

    private IssuedToken createToken(String username, String type, long ttlSeconds) {
        Instant issuedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = issuedAt.plusSeconds(ttlSeconds);
        String token = Jwts.builder()
                .setSubject(username)
                .setId(UUID.randomUUID().toString())
                .claim(TYPE_CLAIM, type)
                .setIssuedAt(Date.from(issuedAt))
                .setExpiration(Date.from(expiresAt))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
        return new IssuedToken(token, expiresAt);
    }

    private Claims validateToken(String token, String expectedType) {
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("Токен отсутствует");
        }
        try {
            Jws<Claims> jwt = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            if (!SignatureAlgorithm.HS256.getValue().equals(jwt.getHeader().getAlgorithm())) {
                throw new InvalidTokenException("Неверный алгоритм подписи");
            }
            Claims claims = jwt.getBody();
            String type = claims.get(TYPE_CLAIM, String.class);
            if (!expectedType.equals(type)) {
                throw new InvalidTokenException("Неверный тип токена");
            }
            if (claims.getSubject() == null || claims.getSubject().isBlank()
                    || claims.getId() == null || claims.getId().isBlank()
                    || claims.getIssuedAt() == null || claims.getExpiration() == null) {
                throw new InvalidTokenException("В токене отсутствуют обязательные данные");
            }
            return claims;
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException("Недействительный токен", ex);
        }
    }

    public static class IssuedToken {
        private final String token;
        private final Instant expiresAt;

        public IssuedToken(String token, Instant expiresAt) {
            this.token = token;
            this.expiresAt = expiresAt;
        }

        public String getToken() {
            return token;
        }

        public Instant getExpiresAt() {
            return expiresAt;
        }
    }
}
