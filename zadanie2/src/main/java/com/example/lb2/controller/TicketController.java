package com.example.lb2.controller;

import com.example.lb2.dto.TicketDto;
import com.example.lb2.entity.Ticket;
import com.example.lb2.service.TicketService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {
    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public ResponseEntity<List<TicketDto>> getAll() {
        List<TicketDto> tickets = ticketService.findAll().stream()
                .map(TicketController::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketDto> getById(@PathVariable Long id) {
        return ticketService.findById(id)
                .map(TicketController::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TicketDto> create(@RequestBody TicketDto dto) {
        Ticket created = ticketService.create(toEntity(dto), dto.getPerformanceId(), dto.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(created));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TicketDto> update(@PathVariable Long id, @RequestBody TicketDto dto) {
        return ticketService.update(id, toEntity(dto), dto.getPerformanceId(), dto.getUserId())
                .map(TicketController::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return ticketService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    private static Ticket toEntity(TicketDto dto) {
        Ticket ticket = new Ticket();
        ticket.setSeatNumber(dto.getSeatNumber());
        ticket.setStatus(dto.getStatus());
        return ticket;
    }

    private static TicketDto toDto(Ticket ticket) {
        TicketDto dto = new TicketDto();
        dto.setId(ticket.getId());
        dto.setPerformanceId(ticket.getPerformance() != null ? ticket.getPerformance().getId() : null);
        dto.setSeatNumber(ticket.getSeatNumber());
        dto.setUserId(ticket.getUser() != null ? ticket.getUser().getId() : null);
        dto.setStatus(ticket.getStatus());
        return dto;
    }
}
