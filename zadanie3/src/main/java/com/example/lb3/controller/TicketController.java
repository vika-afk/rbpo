package com.example.lb3.controller;

import com.example.lb3.dto.BuyTicketRequest;
import com.example.lb3.dto.TicketDto;
import com.example.lb3.entity.Ticket;
import com.example.lb3.entity.TicketStatus;
import com.example.lb3.exception.BusinessRuleException;
import com.example.lb3.service.TicketService;
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

    @PostMapping("/purchase")
    public ResponseEntity<TicketDto> purchase(@RequestBody BuyTicketRequest request) {
        Ticket ticket = ticketService.buyTicket(
                request.getPerformanceId(), request.getUserId(), request.getSeatNumber());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(ticket));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<TicketDto> refund(@PathVariable Long id) {
        return ResponseEntity.ok(toDto(ticketService.refundTicket(id)));
    }

    private static Ticket toEntity(TicketDto dto) {
        Ticket ticket = new Ticket();
        ticket.setSeatNumber(dto.getSeatNumber());
        ticket.setStatus(parseStatus(dto.getStatus()));
        return ticket;
    }

    private static TicketStatus parseStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }
        try {
            return TicketStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Недопустимый статус билета: " + status);
        }
    }

    private static TicketDto toDto(Ticket ticket) {
        TicketDto dto = new TicketDto();
        dto.setId(ticket.getId());
        dto.setPerformanceId(ticket.getPerformance() != null ? ticket.getPerformance().getId() : null);
        dto.setSeatNumber(ticket.getSeatNumber());
        dto.setUserId(ticket.getUser() != null ? ticket.getUser().getId() : null);
        dto.setStatus(ticket.getStatus() != null ? ticket.getStatus().name() : null);
        return dto;
    }
}
