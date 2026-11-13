package com.example.lb3.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginRequest {
    @NotBlank(message = "Email обязателен")
    @Size(max = 255, message = "Email слишком длинный")
    private String email;

    @NotBlank(message = "Пароль обязателен")
    private String password;

    @Size(max = 255, message = "Идентификатор устройства слишком длинный")
    private String deviceId;
}
