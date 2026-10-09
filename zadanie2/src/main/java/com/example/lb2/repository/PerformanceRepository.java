package com.example.lb2.repository;

import com.example.lb2.entity.Performance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PerformanceRepository extends JpaRepository<Performance, Long> {
    List<Performance> findByPlayId(Long playId);
    List<Performance> findByHallId(Long hallId);
}