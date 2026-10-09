package com.example.lb2.service;

import com.example.lb2.entity.Performance;
import com.example.lb2.entity.Play;
import com.example.lb2.repository.PerformanceRepository;
import com.example.lb2.repository.PlayRepository;
import com.example.lb2.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PlayService {
    private final PlayRepository playRepository;
    private final PerformanceRepository performanceRepository;
    private final TicketRepository ticketRepository;

    public PlayService(PlayRepository playRepository,
                       PerformanceRepository performanceRepository,
                       TicketRepository ticketRepository) {
        this.playRepository = playRepository;
        this.performanceRepository = performanceRepository;
        this.ticketRepository = ticketRepository;
    }

    public List<Play> findAll() {
        return playRepository.findAll();
    }

    public Optional<Play> findById(Long id) {
        return playRepository.findById(id);
    }

    @Transactional
    public Play create(Play play) {
        return playRepository.save(play);
    }

    @Transactional
    public Optional<Play> update(Long id, Play changes) {
        return playRepository.findById(id).map(play -> {
            if (changes.getTitle() != null) play.setTitle(changes.getTitle());
            if (changes.getDurationMinutes() != null) play.setDurationMinutes(changes.getDurationMinutes());
            if (changes.getAgeLimit() != null) play.setAgeLimit(changes.getAgeLimit());
            return playRepository.save(play);
        });
    }

    @Transactional
    public boolean delete(Long id) {
        if (!playRepository.existsById(id)) {
            return false;
        }
        List<Performance> performances = performanceRepository.findByPlayId(id);
        for (Performance performance : performances) {
            ticketRepository.deleteAll(ticketRepository.findByPerformanceId(performance.getId()));
        }
        performanceRepository.deleteAll(performances);
        playRepository.deleteById(id);
        return true;
    }
}
