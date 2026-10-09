package com.example.lb2.controller;

import com.example.lb2.dto.TheatreHallDto;
import com.example.lb2.entity.TheatreHall;
import com.example.lb2.service.TheatreHallService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/theatre-halls")
public class TheatreHallController {
    private final TheatreHallService hallService;

    public TheatreHallController(TheatreHallService hallService) {
        this.hallService = hallService;
    }

    @GetMapping
    public ResponseEntity<List<TheatreHallDto>> getAll() {
        List<TheatreHallDto> halls = hallService.findAll().stream()
                .map(TheatreHallController::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(halls);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TheatreHallDto> getById(@PathVariable Long id) {
        return hallService.findById(id)
                .map(TheatreHallController::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TheatreHallDto> create(@RequestBody TheatreHallDto dto) {
        TheatreHall created = hallService.create(toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(created));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TheatreHallDto> update(@PathVariable Long id, @RequestBody TheatreHallDto dto) {
        return hallService.update(id, toEntity(dto))
                .map(TheatreHallController::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return hallService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    private static TheatreHall toEntity(TheatreHallDto dto) {
        TheatreHall hall = new TheatreHall();
        hall.setName(dto.getName());
        hall.setCapacity(dto.getCapacity());
        return hall;
    }

    private static TheatreHallDto toDto(TheatreHall hall) {
        TheatreHallDto dto = new TheatreHallDto();
        dto.setId(hall.getId());
        dto.setName(hall.getName());
        dto.setCapacity(hall.getCapacity());
        return dto;
    }
}
