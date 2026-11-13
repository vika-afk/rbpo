package com.example.lb3.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Email;
import javax.validation.constraints.Size;

@Getter
@Setter
public class UserUpdateRequest {
    @Size(max = 255, message = "Имя слишком длинное")
    private String name;

    @Email(message = "Некорректный email")
    @Size(max = 255, message = "Email слишком длинный")
    private String email;

    private String password;
    private String role;
    private Boolean enabled;
    private Boolean blocked;
}
