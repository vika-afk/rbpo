package com.example.lb3.service;

import com.example.lb3.dto.PerformanceCancellationDto;
import com.example.lb3.entity.*;
import com.example.lb3.exception.BusinessRuleException;
import com.example.lb3.exception.ResourceNotFoundException;
import com.example.lb3.repository.PerformanceRepository;
import com.example.lb3.repository.PlayRepository;
import com.example.lb3.repository.TheatreHallRepository;
import com.example.lb3.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class PerformanceService {

    private final PerformanceRepository performanceRepository;
    private final TicketRepository ticketRepository;
    private final PlayRepository playRepository;
    private final TheatreHallRepository theatreHallRepository;

    public PerformanceService(PerformanceRepository performanceRepository,
                              TicketRepository ticketRepository,
                              PlayRepository playRepository,
                              TheatreHallRepository theatreHallRepository) {
        this.performanceRepository = performanceRepository;
        this.ticketRepository = ticketRepository;
        this.playRepository = playRepository;
        this.theatreHallRepository = theatreHallRepository;
    }

    public List<Performance> findAll() {
        return performanceRepository.findAll();
    }

    public Optional<Performance> findById(Long id) {
        return performanceRepository.findById(id);
    }

    @Transactional
    public Performance create(Performance performance, Long playId, Long hallId) {
        if (playId == null || hallId == null) {
            throw new BusinessRuleException("Не указаны пьеса и зал представления");
        }
        Play play = findPlay(playId);
        TheatreHall hall = findHall(hallId);
        performance.setPlay(play);
        performance.setHall(hall);
        if (performance.getStatus() == null) {
            performance.setStatus(PerformanceStatus.SCHEDULED);
        }
        ensureHallIsFree(hall.getId(), performance, performance.getStartTime());
        return performanceRepository.save(performance);
    }

    @Transactional
    public Optional<Performance> update(Long id, Performance changes, Long playId, Long hallId) {
        return performanceRepository.findById(id).map(performance -> {
            boolean scheduleChange = playId != null || hallId != null || changes.getStartTime() != null;
            if (scheduleChange) {
                if (performance.getStatus() != PerformanceStatus.SCHEDULED) {
                    throw new BusinessRuleException(
                            "Пьесу, зал и время можно менять только у запланированного представления");
                }
                if (playId != null && !playId.equals(performance.getPlay().getId())) {
                    performance.setPlay(findPlay(playId));
                }
                if (hallId != null && !hallId.equals(performance.getHall().getId())) {
                    performance.setHall(findHall(hallId));
                }
                if (changes.getStartTime() != null) {
                    performance.setStartTime(changes.getStartTime());
                }
                if (!performance.getStartTime().isAfter(LocalDateTime.now())) {
                    throw new BusinessRuleException("Время начала представления должно быть в будущем");
                }
                validateSeatsAndHallFree(performance);
            }
            if (changes.getBasePrice() != null) {
                if (changes.getBasePrice().signum() < 0) {
                    throw new BusinessRuleException("Базовая цена не может быть отрицательной");
                }
                performance.setBasePrice(changes.getBasePrice());
            }
            if (changes.getStatus() != null && changes.getStatus() != performance.getStatus()) {
                applyStatusChange(performance, changes.getStatus());
            }
            return performanceRepository.save(performance);
        });
    }

    @Transactional
    public boolean delete(Long id) {
        if (!performanceRepository.existsById(id)) {
            return false;
        }
        if (ticketRepository.existsByPerformanceIdAndStatus(id, TicketStatus.SOLD)) {
            throw new BusinessRuleException("Нельзя удалить представление: по нему есть проданные билеты. "
                    + "Сначала оформите возврат или отмените представление.");
        }
        ticketRepository.deleteAll(ticketRepository.findByPerformanceId(id));
        performanceRepository.deleteById(id);
        return true;
    }

    @Transactional
    public PerformanceCancellationDto cancelPerformance(Long performanceId) {
        Performance performance = performanceRepository.findById(performanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Представление не найдено: " + performanceId));

        if (performance.getStatus() != PerformanceStatus.SCHEDULED) {
            throw new BusinessRuleException("Отменить можно только запланированное представление");
        }
        if (!performance.getStartTime().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("Представление уже началось - отмена невозможна");
        }

        performance.setStatus(PerformanceStatus.CANCELLED);
        performanceRepository.save(performance);

        int refunded = 0;
        int cancelled = 0;
        List<Ticket> tickets = ticketRepository.findByPerformanceIdAndStatusIn(
                performanceId, TicketService.ACTIVE_STATUSES);
        for (Ticket ticket : tickets) {
            if (ticket.getStatus() == TicketStatus.SOLD) {
                ticket.setStatus(TicketStatus.REFUNDED);
                refunded++;
            } else if (ticket.getStatus() == TicketStatus.RESERVED) {
                ticket.setStatus(TicketStatus.CANCELLED);
                cancelled++;
            }
        }
        ticketRepository.saveAll(tickets);

        return new PerformanceCancellationDto(performance.getId(), performance.getStatus().name(),
                refunded, cancelled);
    }

    @Transactional
    public Performance reschedulePerformance(Long performanceId, Long newHallId,
                                             LocalDateTime newStartTime, BigDecimal newBasePrice) {
        Performance performance = performanceRepository.findById(performanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Представление не найдено: " + performanceId));

        if (performance.getStatus() != PerformanceStatus.SCHEDULED) {
            throw new BusinessRuleException("Перенести можно только запланированное представление");
        }
        if (!performance.getStartTime().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("Представление уже началось - перенос невозможен");
        }

        TheatreHall targetHall = performance.getHall();
        if (newHallId != null) {
            targetHall = findHall(newHallId);
        }

        LocalDateTime targetStart = newStartTime != null ? newStartTime : performance.getStartTime();
        if (!targetStart.isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("Новое время начала должно быть в будущем");
        }
        if (newBasePrice != null && newBasePrice.signum() < 0) {
            throw new BusinessRuleException("Базовая цена не может быть отрицательной");
        }

        performance.setHall(targetHall);
        performance.setStartTime(targetStart);
        if (newBasePrice != null) {
            performance.setBasePrice(newBasePrice);
        }
        validateSeatsAndHallFree(performance);
        return performanceRepository.save(performance);
    }

    private void applyStatusChange(Performance performance, PerformanceStatus target) {
        if (target == PerformanceStatus.CANCELLED) {
            throw new BusinessRuleException("Отменить представление можно только операцией отмены: "
                    + "POST /api/performances/{id}/cancel");
        }
        if (target != PerformanceStatus.COMPLETED) {
            throw new BusinessRuleException("Недопустимая смена статуса представления: "
                    + performance.getStatus() + " -> " + target);
        }
        if (performance.getStatus() != PerformanceStatus.SCHEDULED) {
            throw new BusinessRuleException("Завершить можно только запланированное представление");
        }
        if (performance.getStartTime().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("Представление ещё не началось - завершить его нельзя");
        }
        performance.setStatus(PerformanceStatus.COMPLETED);
        List<Ticket> reservedTickets = ticketRepository.findByPerformanceIdAndStatusIn(
                performance.getId(), Collections.singletonList(TicketStatus.RESERVED));
        for (Ticket ticket : reservedTickets) {
            ticket.setStatus(TicketStatus.CANCELLED);
        }
        ticketRepository.saveAll(reservedTickets);
    }

    private void validateSeatsAndHallFree(Performance performance) {
        TheatreHall hall = performance.getHall();
        int maxOccupiedSeat = ticketRepository
                .findByPerformanceIdAndStatusIn(performance.getId(), TicketService.ACTIVE_STATUSES)
                .stream()
                .mapToInt(Ticket::getSeatNumber)
                .max()
                .orElse(0);
        if (maxOccupiedSeat > hall.getCapacity()) {
            throw new BusinessRuleException("В зале '" + hall.getName() + "' недостаточно мест: "
                    + "уже продано место №" + maxOccupiedSeat + ", вместимость " + hall.getCapacity());
        }
        ensureHallIsFree(hall.getId(), performance, performance.getStartTime());
    }

    private void ensureHallIsFree(Long hallId, Performance self, LocalDateTime candidateStart) {
        int selfDuration = durationOf(self.getPlay());
        LocalDateTime candidateEnd = candidateStart.plusMinutes(selfDuration);

        List<Performance> others = performanceRepository.findByHallIdAndStatus(hallId, PerformanceStatus.SCHEDULED);
        for (Performance other : others) {
            if (other.getId() != null && other.getId().equals(self.getId())) {
                continue;
            }
            LocalDateTime otherStart = other.getStartTime();
            LocalDateTime otherEnd = otherStart.plusMinutes(durationOf(other.getPlay()));
            boolean overlap = candidateStart.isBefore(otherEnd) && otherStart.isBefore(candidateEnd);
            if (overlap) {
                throw new BusinessRuleException("В выбранном зале уже запланировано другое представление "
                        + "на пересекающееся время (" + otherStart + ")");
            }
        }
    }

    private int durationOf(Play play) {
        return (play != null && play.getDurationMinutes() != null) ? play.getDurationMinutes() : 0;
    }

    private Play findPlay(Long playId) {
        Play play = playRepository.findById(playId)
                .orElseThrow(() -> new ResourceNotFoundException("Пьеса не найдена: " + playId));
        if (play.isArchived()) {
            throw new BusinessRuleException(
                    "Пьеса снята с репертуара - представление нельзя создать или перенести на неё");
        }
        return play;
    }

    private TheatreHall findHall(Long hallId) {
        return theatreHallRepository.findById(hallId)
                .orElseThrow(() -> new ResourceNotFoundException("Зал не найден: " + hallId));
    }
}
