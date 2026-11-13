package com.example.lb3.service;

import com.example.lb3.entity.Performance;
import com.example.lb3.entity.TheatreHall;
import com.example.lb3.entity.TicketStatus;
import com.example.lb3.exception.BusinessRuleException;
import com.example.lb3.repository.PerformanceRepository;
import com.example.lb3.repository.TheatreHallRepository;
import com.example.lb3.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TheatreHallService {
    private final TheatreHallRepository hallRepository;
    private final PerformanceRepository performanceRepository;
    private final TicketRepository ticketRepository;

    public TheatreHallService(TheatreHallRepository hallRepository,
                              PerformanceRepository performanceRepository,
                              TicketRepository ticketRepository) {
        this.hallRepository = hallRepository;
        this.performanceRepository = performanceRepository;
        this.ticketRepository = ticketRepository;
    }

    public List<TheatreHall> findAll() {
        return hallRepository.findAll();
    }

    public Optional<TheatreHall> findById(Long id) {
        return hallRepository.findById(id);
    }

    @Transactional
    public TheatreHall create(TheatreHall hall) {
        return hallRepository.save(hall);
    }

    @Transactional
    public Optional<TheatreHall> update(Long id, TheatreHall changes) {
        return hallRepository.findById(id).map(hall -> {
            if (changes.getName() != null) hall.setName(changes.getName());
            if (changes.getCapacity() != null) {
                Integer maxOccupiedSeat = ticketRepository.findMaxActiveSeatNumberInHall(
                        hall.getId(), TicketService.ACTIVE_STATUSES);
                if (maxOccupiedSeat != null && maxOccupiedSeat > changes.getCapacity()) {
                    throw new BusinessRuleException("В зале '" + hall.getName() + "' недостаточно мест: "
                            + "уже продано место №" + maxOccupiedSeat
                            + ", новая вместимость " + changes.getCapacity());
                }
                hall.setCapacity(changes.getCapacity());
            }
            return hallRepository.save(hall);
        });
    }

    @Transactional
    public boolean delete(Long id) {
        if (!hallRepository.existsById(id)) {
            return false;
        }
        List<Performance> performances = performanceRepository.findByHallId(id);
        for (Performance performance : performances) {
            if (ticketRepository.existsByPerformanceIdAndStatus(performance.getId(), TicketStatus.SOLD)) {
                throw new BusinessRuleException("Нельзя удалить зал: по его представлениям есть проданные билеты. "
                        + "Сначала оформите возврат или отмените представления.");
            }
        }
        for (Performance performance : performances) {
            ticketRepository.deleteAll(ticketRepository.findByPerformanceId(performance.getId()));
        }
        performanceRepository.deleteAll(performances);
        hallRepository.deleteById(id);
        return true;
    }
}
