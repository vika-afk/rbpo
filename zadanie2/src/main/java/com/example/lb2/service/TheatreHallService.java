package com.example.lb2.service;

import com.example.lb2.entity.Performance;
import com.example.lb2.entity.TheatreHall;
import com.example.lb2.repository.PerformanceRepository;
import com.example.lb2.repository.TheatreHallRepository;
import com.example.lb2.repository.TicketRepository;
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
            if (changes.getCapacity() != null) hall.setCapacity(changes.getCapacity());
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
            ticketRepository.deleteAll(ticketRepository.findByPerformanceId(performance.getId()));
        }
        performanceRepository.deleteAll(performances);
        hallRepository.deleteById(id);
        return true;
    }
}
