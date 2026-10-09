package com.example.lb2.repository;

import com.example.lb2.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByPerformanceId(Long performanceId);
    List<Ticket> findByUserId(Long userId);
}