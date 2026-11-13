package com.example.lb3.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RefreshRequest {
    @NotBlank(message = "Refresh-токен обязателен")
    @JsonAlias("refresh_token")
    private String refreshToken;
}
