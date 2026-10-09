package com.example.lb2.service;

import com.example.lb2.entity.Performance;
import com.example.lb2.entity.Play;
import com.example.lb2.entity.TheatreHall;
import com.example.lb2.repository.PerformanceRepository;
import com.example.lb2.repository.PlayRepository;
import com.example.lb2.repository.TheatreHallRepository;
import com.example.lb2.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        performance.setPlay(resolvePlay(playId));
        performance.setHall(resolveHall(hallId));
        return performanceRepository.save(performance);
    }

    @Transactional
    public Optional<Performance> update(Long id, Performance changes, Long playId, Long hallId) {
        return performanceRepository.findById(id).map(performance -> {
            if (playId != null) performance.setPlay(resolvePlay(playId));
            if (hallId != null) performance.setHall(resolveHall(hallId));
            if (changes.getStartTime() != null) performance.setStartTime(changes.getStartTime());
            if (changes.getBasePrice() != null) performance.setBasePrice(changes.getBasePrice());
            if (changes.getStatus() != null) performance.setStatus(changes.getStatus());
            return performanceRepository.save(performance);
        });
    }

    @Transactional
    public boolean delete(Long id) {
        if (!performanceRepository.existsById(id)) {
            return false;
        }
        ticketRepository.deleteAll(ticketRepository.findByPerformanceId(id));
        performanceRepository.deleteById(id);
        return true;
    }

    private Play resolvePlay(Long playId) {
        return playId == null ? null : playRepository.findById(playId).orElse(null);
    }

    private TheatreHall resolveHall(Long hallId) {
        return hallId == null ? null : theatreHallRepository.findById(hallId).orElse(null);
    }
}
