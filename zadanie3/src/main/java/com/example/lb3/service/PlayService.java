package com.example.lb3.service;

import com.example.lb3.entity.Performance;
import com.example.lb3.entity.PerformanceStatus;
import com.example.lb3.entity.Play;
import com.example.lb3.entity.TicketStatus;
import com.example.lb3.exception.BusinessRuleException;
import com.example.lb3.exception.ResourceNotFoundException;
import com.example.lb3.repository.PerformanceRepository;
import com.example.lb3.repository.PlayRepository;
import com.example.lb3.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
            if (changes.isArchived() && !play.isArchived()) {
                throw new BusinessRuleException("Снять пьесу с репертуара можно только операцией "
                        + "POST /api/plays/{id}/archive");
            }
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
            if (ticketRepository.existsByPerformanceIdAndStatus(performance.getId(), TicketStatus.SOLD)) {
                throw new BusinessRuleException("Нельзя удалить пьесу: по её представлениям есть проданные билеты. "
                        + "Сначала оформите возврат или отмените представления.");
            }
        }
        for (Performance performance : performances) {
            ticketRepository.deleteAll(ticketRepository.findByPerformanceId(performance.getId()));
        }
        performanceRepository.deleteAll(performances);
        playRepository.deleteById(id);
        return true;
    }

    @Transactional
    public Play archivePlay(Long playId) {
        Play play = playRepository.findById(playId)
                .orElseThrow(() -> new ResourceNotFoundException("Пьеса не найдена: " + playId));

        if (play.isArchived()) {
            throw new BusinessRuleException("Пьеса уже снята с репертуара");
        }

        boolean hasFuturePerformances = performanceRepository
                .existsByPlayIdAndStatusAndStartTimeAfter(playId, PerformanceStatus.SCHEDULED, LocalDateTime.now());
        if (hasFuturePerformances) {
            throw new BusinessRuleException("Нельзя снять пьесу: есть запланированные представления. "
                    + "Сначала отмените или завершите их.");
        }

        play.setArchived(true);
        return playRepository.save(play);
    }
}
