package com.example.lb3.exception;

import org.springframework.security.core.AuthenticationException;

public class TokenReuseException extends AuthenticationException {
    public TokenReuseException(String message) {
        super(message);
    }
}
