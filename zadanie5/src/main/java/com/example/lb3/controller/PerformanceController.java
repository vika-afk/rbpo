package com.example.lb3.controller;

import com.example.lb3.dto.PerformanceCancellationDto;
import com.example.lb3.dto.PerformanceDto;
import com.example.lb3.dto.ReschedulePerformanceRequest;
import com.example.lb3.entity.Performance;
import com.example.lb3.entity.PerformanceStatus;
import com.example.lb3.exception.BusinessRuleException;
import com.example.lb3.service.PerformanceService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/performances")
public class PerformanceController {
    private final PerformanceService performanceService;

    public PerformanceController(PerformanceService performanceService) {
        this.performanceService = performanceService;
    }

    @GetMapping
    public ResponseEntity<List<PerformanceDto>> getAll() {
        List<PerformanceDto> performances = performanceService.findAll().stream()
                .map(PerformanceController::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(performances);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PerformanceDto> getById(@PathVariable Long id) {
        return performanceService.findById(id)
                .map(PerformanceController::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping
    public ResponseEntity<PerformanceDto> create(@RequestBody PerformanceDto dto) {
        Performance created = performanceService.create(toEntity(dto), dto.getPlayId(), dto.getHallId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(created));
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PatchMapping("/{id}")
    public ResponseEntity<PerformanceDto> update(@PathVariable Long id, @RequestBody PerformanceDto dto) {
        return performanceService.update(id, toEntity(dto), dto.getPlayId(), dto.getHallId())
                .map(PerformanceController::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return performanceService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<PerformanceCancellationDto> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(performanceService.cancelPerformance(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping("/{id}/reschedule")
    public ResponseEntity<PerformanceDto> reschedule(@PathVariable Long id,
                                                     @RequestBody ReschedulePerformanceRequest request) {
        Performance updated = performanceService.reschedulePerformance(
                id, request.getHallId(), request.getStartTime(), request.getBasePrice());
        return ResponseEntity.ok(toDto(updated));
    }

    private static Performance toEntity(PerformanceDto dto) {
        Performance performance = new Performance();
        performance.setStartTime(dto.getStartTime());
        performance.setBasePrice(dto.getBasePrice());
        performance.setStatus(parseStatus(dto.getStatus()));
        return performance;
    }

    private static PerformanceStatus parseStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }
        try {
            return PerformanceStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Недопустимый статус представления: " + status);
        }
    }

    private static PerformanceDto toDto(Performance performance) {
        PerformanceDto dto = new PerformanceDto();
        dto.setId(performance.getId());
        dto.setPlayId(performance.getPlay() != null ? performance.getPlay().getId() : null);
        dto.setHallId(performance.getHall() != null ? performance.getHall().getId() : null);
        dto.setStartTime(performance.getStartTime());
        dto.setBasePrice(performance.getBasePrice());
        dto.setStatus(performance.getStatus() != null ? performance.getStatus().name() : null);
        return dto;
    }
}
