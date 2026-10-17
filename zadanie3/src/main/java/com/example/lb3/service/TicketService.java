package com.example.lb3.service;

import com.example.lb3.entity.*;
import com.example.lb3.exception.BusinessRuleException;
import com.example.lb3.exception.ResourceNotFoundException;
import com.example.lb3.repository.PerformanceRepository;
import com.example.lb3.repository.TicketRepository;
import com.example.lb3.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class TicketService {

    public static final List<TicketStatus> ACTIVE_STATUSES =
            Arrays.asList(TicketStatus.RESERVED, TicketStatus.SOLD);

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
        if (performanceId == null) {
            throw new BusinessRuleException("Не указано представление");
        }
        Performance performance = performanceRepository.findById(performanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Представление не найдено: " + performanceId));
        if (userId != null) {
            ticket.setUser(userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Зритель не найден: " + userId)));
        }
        ticket.setPerformance(performance);
        if (ticket.getSeatNumber() == null || ticket.getSeatNumber() < 1) {
            throw new BusinessRuleException("Номер места должен быть больше нуля");
        }
        if (ticket.getSeatNumber() > performance.getHall().getCapacity()) {
            throw new BusinessRuleException("В зале '" + performance.getHall().getName()
                    + "' нет места с номером " + ticket.getSeatNumber());
        }
        if (ticket.getStatus() == null) {
            ticket.setStatus(TicketStatus.RESERVED);
        }
        return ticketRepository.save(ticket);
    }

    @Transactional
    public Optional<Ticket> update(Long id, Ticket changes, Long performanceId, Long userId) {
        return ticketRepository.findById(id).map(ticket -> {
            if (changes.getStatus() != null && changes.getStatus() != ticket.getStatus()) {
                throw new BusinessRuleException("Статус билета меняется только операциями: "
                        + "продажа, возврат или отмена представления");
            }
            if (performanceId != null && !performanceId.equals(ticket.getPerformance().getId())) {
                if (ticket.getStatus() == TicketStatus.SOLD) {
                    throw new BusinessRuleException(
                            "Проданный билет нельзя перевести на другое представление - оформите возврат");
                }
                ticket.setPerformance(performanceRepository.findById(performanceId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Представление не найдено: " + performanceId)));
            }
            if (userId != null) {
                ticket.setUser(userRepository.findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Зритель не найден: " + userId)));
            }
            if (changes.getSeatNumber() != null) {
                ticket.setSeatNumber(changes.getSeatNumber());
            }
            TheatreHall hall = ticket.getPerformance().getHall();
            if (ticket.getSeatNumber() < 1 || ticket.getSeatNumber() > hall.getCapacity()) {
                throw new BusinessRuleException(
                        "В зале '" + hall.getName() + "' нет места с номером " + ticket.getSeatNumber());
            }
            return ticketRepository.save(ticket);
        });
    }

    @Transactional
    public boolean delete(Long id) {
        Ticket ticket = ticketRepository.findById(id).orElse(null);
        if (ticket == null) {
            return false;
        }
        if (ticket.getStatus() == TicketStatus.SOLD) {
            throw new BusinessRuleException("Нельзя удалить проданный билет - оформите возврат");
        }
        ticketRepository.deleteById(id);
        return true;
    }

    @Transactional
    public Ticket buyTicket(Long performanceId, Long userId, Integer seatNumber) {
        if (performanceId == null || userId == null || seatNumber == null) {
            throw new BusinessRuleException("Не указаны представление, зритель или место");
        }

        Performance performance = performanceRepository.findById(performanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Представление не найдено: " + performanceId));

        if (performance.getStatus() != PerformanceStatus.SCHEDULED) {
            throw new BusinessRuleException("Продажа билетов возможна только на запланированное представление");
        }
        if (!performance.getStartTime().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("Представление уже началось - продажа билетов закрыта");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Зритель не найден: " + userId));

        if (seatNumber < 1) {
            throw new BusinessRuleException("Номер места должен быть больше нуля");
        }
        if (performance.getHall() != null && seatNumber > performance.getHall().getCapacity()) {
            throw new BusinessRuleException(
                    "В зале '" + performance.getHall().getName() + "' нет места с номером " + seatNumber);
        }

        if (ticketRepository.existsByPerformanceIdAndSeatNumberAndStatusIn(performanceId, seatNumber, ACTIVE_STATUSES)) {
            throw new BusinessRuleException("Место " + seatNumber + " уже занято на это представление");
        }

        Ticket ticket = new Ticket();
        ticket.setPerformance(performance);
        ticket.setUser(user);
        ticket.setSeatNumber(seatNumber);
        ticket.setStatus(TicketStatus.SOLD);
        return ticketRepository.save(ticket);
    }

    @Transactional
    public Ticket refundTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Билет не найден: " + ticketId));

        if (ticket.getStatus() != TicketStatus.SOLD) {
            throw new BusinessRuleException("Вернуть можно только проданный билет (текущий статус: "
                    + ticket.getStatus() + ")");
        }

        Performance performance = ticket.getPerformance();
        if (performance == null || !performance.getStartTime().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("Возврат невозможен: представление уже началось");
        }
        if (performance.getStatus() == PerformanceStatus.CANCELLED) {
            throw new BusinessRuleException("Представление отменено - билет будет возвращён автоматически");
        }

        ticket.setStatus(TicketStatus.REFUNDED);
        return ticketRepository.save(ticket);
    }
}
