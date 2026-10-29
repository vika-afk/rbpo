package com.example.lb3.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegisterRequest {
    @NotBlank(message = "Имя обязательно")
    @Size(max = 255, message = "Имя слишком длинное")
    private String name;

    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный email")
    @Size(max = 255, message = "Email слишком длинный")
    private String email;

    @NotBlank(message = "Пароль обязателен")
    private String password;
}
