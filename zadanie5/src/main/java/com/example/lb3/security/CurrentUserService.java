package com.example.lb3.security;

import com.example.lb3.entity.User;
import com.example.lb3.exception.ResourceNotFoundException;
import com.example.lb3.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationCredentialsNotFoundException("Требуется аутентификация");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }

    public String currentEmail() {
        return requireCurrentUser().getEmail();
    }

    public boolean isStaff() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String value = authority.getAuthority();
            if ("ROLE_ADMIN".equals(value) || "ROLE_MANAGER".equals(value)) {
                return true;
            }
        }
        return false;
    }
}
