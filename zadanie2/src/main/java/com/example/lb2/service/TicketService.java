package com.example.lb2.service;

import com.example.lb2.entity.Performance;
import com.example.lb2.entity.Ticket;
import com.example.lb2.entity.User;
import com.example.lb2.repository.PerformanceRepository;
import com.example.lb2.repository.TicketRepository;
import com.example.lb2.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TicketService {
    private final TicketRepository ticketRepository;
    private final PerformanceRepository performanceRepository;
    private final UserRepository userRepository;

    public TicketService(TicketRepository ticketRepository,
                         PerformanceRepository performanceRepository,
                         UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.performanceRepository = performanceRepository;
        this.userRepository = userRepository;
    }

    public List<Ticket> findAll() {
        return ticketRepository.findAll();
    }

    public Optional<Ticket> findById(Long id) {
        return ticketRepository.findById(id);
    }

    @Transactional
    public Ticket create(Ticket ticket, Long performanceId, Long userId) {
        ticket.setPerformance(resolvePerformance(performanceId));
        ticket.setUser(resolveUser(userId));
        return ticketRepository.save(ticket);
    }

    @Transactional
    public Optional<Ticket> update(Long id, Ticket changes, Long performanceId, Long userId) {
        return ticketRepository.findById(id).map(ticket -> {
            if (performanceId != null) ticket.setPerformance(resolvePerformance(performanceId));
            if (changes.getSeatNumber() != null) ticket.setSeatNumber(changes.getSeatNumber());
            if (userId != null) ticket.setUser(resolveUser(userId));
            if (changes.getStatus() != null) ticket.setStatus(changes.getStatus());
            return ticketRepository.save(ticket);
        });
    }

    @Transactional
    public boolean delete(Long id) {
        if (!ticketRepository.existsById(id)) {
            return false;
        }
        ticketRepository.deleteById(id);
        return true;
    }

    private Performance resolvePerformance(Long performanceId) {
        return performanceId == null ? null : performanceRepository.findById(performanceId).orElse(null);
    }

    private User resolveUser(Long userId) {
        return userId == null ? null : userRepository.findById(userId).orElse(null);
    }
}
