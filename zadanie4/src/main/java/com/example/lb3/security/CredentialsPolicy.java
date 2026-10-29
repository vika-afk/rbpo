package com.example.lb3.security;

import com.example.lb3.exception.ValidationException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class CredentialsPolicy {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PASSWORD = Pattern.compile(
            "^(?=.*\\p{Lu})(?=.*\\p{Ll})(?=.*\\d)(?=.*[^\\p{L}\\d]).{8,}$");
    private static final Set<String> ROLES = Set.of("ADMIN", "MANAGER", "USER");

    private CredentialsPolicy() {
    }

    public static String normalizeEmail(String email) {
        if (email == null) {
            throw new ValidationException("Email обязателен");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(normalized).matches()) {
            throw new ValidationException("Некорректный email");
        }
        if (normalized.length() > 255) {
            throw new ValidationException("Email слишком длинный");
        }
        return normalized;
    }

    public static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Имя обязательно");
        }
        if (name.trim().length() > 255) {
            throw new ValidationException("Имя слишком длинное");
        }
    }

    public static void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new ValidationException("Пароль обязателен");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ValidationException("Пароль не должен превышать 72 байта");
        }
        if (!PASSWORD.matcher(password).matches()) {
            throw new ValidationException(
                    "Пароль должен содержать минимум 8 символов, заглавную и строчную буквы, цифру и спецсимвол");
        }
    }

    public static String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            throw new ValidationException("Роль обязательна");
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        if (!ROLES.contains(normalized)) {
            throw new ValidationException("Допустимые роли: ADMIN, MANAGER, USER");
        }
        return normalized;
    }
}
