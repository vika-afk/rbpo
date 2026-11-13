package com.example.lb3;

import com.example.lb3.dto.TokenPairDto;
import com.example.lb3.entity.SessionStatus;
import com.example.lb3.entity.User;
import com.example.lb3.exception.InvalidTokenException;
import com.example.lb3.exception.TokenReuseException;
import com.example.lb3.repository.UserRepository;
import com.example.lb3.repository.UserSessionRepository;
import com.example.lb3.security.JwtService;
import com.example.lb3.service.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TokenServiceTest {

    @Autowired private TokenService tokenService;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;
    @Autowired private UserSessionRepository sessionRepository;

    @Test
    void refreshCreatesNewSessionAndRevokesReusedToken() {
        User user = createUser();
        TokenPairDto initial = tokenService.issueTokens(user, "browser");
        TokenPairDto renewed = tokenService.refreshTokens(initial.getRefreshToken());

        assertNotEquals(initial.getAccessToken(), renewed.getAccessToken());
        assertNotEquals(initial.getRefreshToken(), renewed.getRefreshToken());
        assertEquals("browser", renewed.getDeviceId());
        assertEquals(SessionStatus.USED, sessionRepository
                .findByRefreshToken(initial.getRefreshToken()).orElseThrow().getStatus());
        assertEquals(SessionStatus.ACTIVE, sessionRepository
                .findByRefreshToken(renewed.getRefreshToken()).orElseThrow().getStatus());

        assertThrows(TokenReuseException.class,
                () -> tokenService.refreshTokens(initial.getRefreshToken()));
        assertEquals(SessionStatus.REVOKED, sessionRepository
                .findByRefreshToken(initial.getRefreshToken()).orElseThrow().getStatus());
        assertThrows(InvalidTokenException.class,
                () -> tokenService.refreshTokens(initial.getRefreshToken()));
    }

    @Test
    void accessCannotBeUsedAsRefresh() {
        User user = createUser();
        TokenPairDto pair = tokenService.issueTokens(user, null);
        assertThrows(InvalidTokenException.class,
                () -> tokenService.refreshTokens(pair.getAccessToken()));
        assertThrows(InvalidTokenException.class,
                () -> jwtService.validateAccessToken(pair.getRefreshToken()));

        Instant accessExpiry = jwtService.validateAccessToken(pair.getAccessToken())
                .getExpiration().toInstant();
        assertEquals(accessExpiry, pair.getAccessTokenExpiry());
    }

    private User createUser() {
        User user = new User();
        user.setName("Test user");
        user.setEmail(UUID.randomUUID() + "@example.test");
        user.setPassword("unused");
        user.setRole("USER");
        user.setEnabled(true);
        user.setBlocked(false);
        return userRepository.save(user);
    }
}
