package com.example.lb3.service;

import com.example.lb3.dto.TokenPairDto;
import com.example.lb3.entity.SessionStatus;
import com.example.lb3.entity.User;
import com.example.lb3.entity.UserSession;
import com.example.lb3.exception.InvalidTokenException;
import com.example.lb3.exception.TokenReuseException;
import com.example.lb3.repository.UserRepository;
import com.example.lb3.repository.UserSessionRepository;
import com.example.lb3.security.JwtService;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class TokenService {

    private final UserSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public TokenService(UserSessionRepository sessionRepository,
                        UserRepository userRepository,
                        JwtService jwtService) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public TokenPairDto issueTokens(User user, String deviceId) {
        return createPair(user, deviceId);
    }

    @Transactional(noRollbackFor = TokenReuseException.class)
    public TokenPairDto refreshTokens(String refreshToken) {
        Claims claims = jwtService.validateRefreshToken(refreshToken);
        UserSession session = sessionRepository.findByRefreshTokenForUpdate(refreshToken)
                .orElseThrow(() -> new InvalidTokenException("Сессия не найдена"));

        if (session.getStatus() == SessionStatus.USED) {
            session.setStatus(SessionStatus.REVOKED);
            sessionRepository.saveAndFlush(session);
            throw new TokenReuseException("Refresh-токен уже использован");
        }
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new InvalidTokenException("Сессия неактивна");
        }
        if (!session.getUsername().equals(claims.getSubject())) {
            throw new InvalidTokenException("Токен принадлежит другому пользователю");
        }
        if (session.getRefreshTokenExpiry() == null
                || !session.getRefreshTokenExpiry().isAfter(Instant.now())) {
            throw new InvalidTokenException("Срок действия refresh-токена истёк");
        }

        User user = userRepository.findByEmail(session.getUsername())
                .orElseThrow(() -> new InvalidTokenException("Пользователь не найден"));
        if (!Boolean.TRUE.equals(user.getEnabled()) || Boolean.TRUE.equals(user.getBlocked())) {
            throw new InvalidTokenException("Пользователь неактивен");
        }

        session.setStatus(SessionStatus.USED);
        sessionRepository.saveAndFlush(session);
        return createPair(user, session.getDeviceId());
    }

    private TokenPairDto createPair(User user, String deviceId) {
        JwtService.IssuedToken access = jwtService.createAccessToken(user.getEmail());
        JwtService.IssuedToken refresh = jwtService.createRefreshToken(user.getEmail());

        UserSession session = new UserSession();
        session.setUsername(user.getEmail());
        session.setDeviceId(deviceId);
        session.setAccessToken(access.getToken());
        session.setRefreshToken(refresh.getToken());
        session.setAccessTokenExpiry(access.getExpiresAt());
        session.setRefreshTokenExpiry(refresh.getExpiresAt());
        session.setStatus(SessionStatus.ACTIVE);
        sessionRepository.saveAndFlush(session);

        TokenPairDto result = new TokenPairDto();
        result.setAccessToken(access.getToken());
        result.setRefreshToken(refresh.getToken());
        result.setAccessTokenExpiry(access.getExpiresAt());
        result.setRefreshTokenExpiry(refresh.getExpiresAt());
        result.setDeviceId(deviceId);
        return result;
    }
}
