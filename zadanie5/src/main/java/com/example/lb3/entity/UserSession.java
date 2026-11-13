package com.example.lb3.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_sessions")
@Getter
@Setter
@NoArgsConstructor
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String username;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "access_token", nullable = false, columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "refresh_token", nullable = false, columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "access_token_expiry", nullable = false)
    private Instant accessTokenExpiry;

    @Column(name = "refresh_token_expiry", nullable = false)
    private Instant refreshTokenExpiry;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status;
}
