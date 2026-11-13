package com.example.lb3.service;

import com.example.lb3.entity.Ticket;
import com.example.lb3.entity.User;
import com.example.lb3.exception.BusinessRuleException;
import com.example.lb3.repository.TicketRepository;
import com.example.lb3.repository.UserRepository;
import com.example.lb3.security.CredentialsPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, TicketRepository ticketRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional
    public User createUser(String name, String email, String password, String role,
                           Boolean enabled, Boolean blocked) {
        CredentialsPolicy.validateName(name);
        String normalizedEmail = CredentialsPolicy.normalizeEmail(email);
        CredentialsPolicy.validatePassword(password);
        String normalizedRole = CredentialsPolicy.normalizeRole(role);
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new BusinessRuleException("Учётная запись с таким email уже существует");
        }
        User user = new User();
        user.setName(name.trim());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(normalizedRole);
        user.setEnabled(enabled == null || enabled);
        user.setBlocked(blocked != null && blocked);
        return userRepository.save(user);
    }

    @Transactional
    public Optional<User> updateUser(Long id, String name, String email, String password, String role,
                                     Boolean enabled, Boolean blocked) {
        return userRepository.findById(id).map(user -> {
            if (name != null) {
                CredentialsPolicy.validateName(name);
                user.setName(name.trim());
            }
            if (email != null) {
                String normalizedEmail = CredentialsPolicy.normalizeEmail(email);
                if (!normalizedEmail.equals(user.getEmail())
                        && userRepository.findByEmail(normalizedEmail).isPresent()) {
                    throw new BusinessRuleException("Учётная запись с таким email уже существует");
                }
                user.setEmail(normalizedEmail);
            }
            if (password != null && !password.isBlank()) {
                CredentialsPolicy.validatePassword(password);
                user.setPassword(passwordEncoder.encode(password));
            }
            if (role != null) {
                user.setRole(CredentialsPolicy.normalizeRole(role));
            }
            if (enabled != null) {
                user.setEnabled(enabled);
            }
            if (blocked != null) {
                user.setBlocked(blocked);
            }
            return userRepository.save(user);
        });
    }

    @Transactional
    public boolean delete(Long id) {
        if (!userRepository.existsById(id)) {
            return false;
        }
        List<Ticket> tickets = ticketRepository.findByUserId(id);
        for (Ticket ticket : tickets) {
            ticket.setUser(null);
            ticketRepository.save(ticket);
        }
        userRepository.deleteById(id);
        return true;
    }
}
