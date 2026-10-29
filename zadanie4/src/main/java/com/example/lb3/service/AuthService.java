package com.example.lb3.service;

import com.example.lb3.entity.User;
import com.example.lb3.exception.BusinessRuleException;
import com.example.lb3.repository.UserRepository;
import com.example.lb3.security.CredentialsPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(String name, String email, String password) {
        CredentialsPolicy.validateName(name);
        String normalizedEmail = CredentialsPolicy.normalizeEmail(email);
        CredentialsPolicy.validatePassword(password);
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new BusinessRuleException("Учётная запись с таким email уже существует");
        }
        User user = new User();
        user.setName(name.trim());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("USER");
        user.setEnabled(true);
        user.setBlocked(false);
        return userRepository.save(user);
    }
}
