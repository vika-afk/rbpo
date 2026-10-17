package com.example.lb3.service;

import com.example.lb3.entity.Ticket;
import com.example.lb3.entity.User;
import com.example.lb3.repository.TicketRepository;
import com.example.lb3.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;

    public UserService(UserRepository userRepository, TicketRepository ticketRepository) {
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional
    public User create(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public Optional<User> update(Long id, User changes) {
        return userRepository.findById(id).map(user -> {
            if (changes.getName() != null) user.setName(changes.getName());
            if (changes.getEmail() != null) user.setEmail(changes.getEmail());
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
