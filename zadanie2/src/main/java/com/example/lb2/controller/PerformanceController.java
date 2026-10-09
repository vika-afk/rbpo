package com.example.lb2.controller;

import com.example.lb2.dto.PerformanceDto;
import com.example.lb2.entity.Performance;
import com.example.lb2.service.PerformanceService;
import org.springframework.http.HttpStatus;
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

    @PostMapping
    public ResponseEntity<PerformanceDto> create(@RequestBody PerformanceDto dto) {
        Performance created = performanceService.create(toEntity(dto), dto.getPlayId(), dto.getHallId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(created));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PerformanceDto> update(@PathVariable Long id, @RequestBody PerformanceDto dto) {
        return performanceService.update(id, toEntity(dto), dto.getPlayId(), dto.getHallId())
                .map(PerformanceController::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return performanceService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    private static Performance toEntity(PerformanceDto dto) {
        Performance performance = new Performance();
        performance.setStartTime(dto.getStartTime());
        performance.setBasePrice(dto.getBasePrice());
        performance.setStatus(dto.getStatus());
        return performance;
    }

    private static PerformanceDto toDto(Performance performance) {
        PerformanceDto dto = new PerformanceDto();
        dto.setId(performance.getId());
        dto.setPlayId(performance.getPlay() != null ? performance.getPlay().getId() : null);
        dto.setHallId(performance.getHall() != null ? performance.getHall().getId() : null);
        dto.setStartTime(performance.getStartTime());
        dto.setBasePrice(performance.getBasePrice());
        dto.setStatus(performance.getStatus());
        return dto;
    }
}
