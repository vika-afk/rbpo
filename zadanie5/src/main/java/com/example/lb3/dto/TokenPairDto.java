package com.example.lb3.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class TokenPairDto {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Instant accessTokenExpiry;
    private Instant refreshTokenExpiry;
    private String deviceId;
}
