package com.example.lb3.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
@Setter
public class UserCreateRequest {
    @NotBlank(message = "Имя обязательно")
    @Size(max = 255, message = "Имя слишком длинное")
    private String name;

    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный email")
    @Size(max = 255, message = "Email слишком длинный")
    private String email;

    @NotBlank(message = "Пароль обязателен")
    private String password;

    @NotBlank(message = "Роль обязательна")
    private String role;

    private Boolean enabled;
    private Boolean blocked;
}
