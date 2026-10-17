package com.example.lb3.repository;

import com.example.lb3.entity.Performance;
import com.example.lb3.entity.PerformanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PerformanceRepository extends JpaRepository<Performance, Long> {
    List<Performance> findByPlayId(Long playId);

    List<Performance> findByHallId(Long hallId);

    List<Performance> findByHallIdAndStatus(Long hallId, PerformanceStatus status);

    boolean existsByPlayIdAndStatusAndStartTimeAfter(Long playId, PerformanceStatus status, LocalDateTime time);
}
