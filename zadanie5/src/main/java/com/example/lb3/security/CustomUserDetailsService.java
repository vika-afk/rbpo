package com.example.lb3.security;

import com.example.lb3.entity.User;
import com.example.lb3.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("user not found"));
        boolean disabled = Boolean.TRUE.equals(user.getBlocked()) || Boolean.FALSE.equals(user.getEnabled());
        return org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole())))
                .disabled(disabled)
                .build();
    }
}
