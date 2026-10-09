package com.example.lb2.controller;

import com.example.lb2.dto.PlayDto;
import com.example.lb2.entity.Play;
import com.example.lb2.service.PlayService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/plays")
public class PlayController {
    private final PlayService playService;

    public PlayController(PlayService playService) {
        this.playService = playService;
    }

    @GetMapping
    public ResponseEntity<List<PlayDto>> getAll() {
        List<PlayDto> plays = playService.findAll().stream()
                .map(PlayController::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(plays);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlayDto> getById(@PathVariable Long id) {
        return playService.findById(id)
                .map(PlayController::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PlayDto> create(@RequestBody PlayDto dto) {
        Play created = playService.create(toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(created));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PlayDto> update(@PathVariable Long id, @RequestBody PlayDto dto) {
        return playService.update(id, toEntity(dto))
                .map(PlayController::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return playService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    private static Play toEntity(PlayDto dto) {
        Play play = new Play();
        play.setTitle(dto.getTitle());
        play.setDurationMinutes(dto.getDurationMinutes());
        play.setAgeLimit(dto.getAgeLimit());
        return play;
    }

    private static PlayDto toDto(Play play) {
        PlayDto dto = new PlayDto();
        dto.setId(play.getId());
        dto.setTitle(play.getTitle());
        dto.setDurationMinutes(play.getDurationMinutes());
        dto.setAgeLimit(play.getAgeLimit());
        return dto;
    }
}
