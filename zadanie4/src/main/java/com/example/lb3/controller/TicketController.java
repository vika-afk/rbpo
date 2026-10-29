package com.example.lb3.controller;

import com.example.lb3.dto.BuyTicketRequest;
import com.example.lb3.dto.TicketDto;
import com.example.lb3.entity.Ticket;
import com.example.lb3.entity.TicketStatus;
import com.example.lb3.entity.User;
import com.example.lb3.exception.BusinessRuleException;
import com.example.lb3.security.CurrentUserService;
import com.example.lb3.service.TicketService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {
    private final TicketService ticketService;
    private final CurrentUserService currentUserService;

    public TicketController(TicketService ticketService, CurrentUserService currentUserService) {
        this.ticketService = ticketService;
        this.currentUserService = currentUserService;
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @GetMapping
    public ResponseEntity<List<TicketDto>> getAll() {
        List<TicketDto> tickets = ticketService.findAll().stream()
                .map(TicketController::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/my")
    public ResponseEntity<List<TicketDto>> getMyTickets() {
        User user = currentUserService.requireCurrentUser();
        List<TicketDto> tickets = ticketService.findByUserId(user.getId()).stream()
                .map(TicketController::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(tickets);
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @ticketAccess.isOwner(#id, authentication.name)")
    @GetMapping("/{id}")
    public ResponseEntity<TicketDto> getById(@PathVariable @P("id") Long id) {
        return ticketService.findById(id)
                .map(TicketController::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping
    public ResponseEntity<TicketDto> create(@RequestBody TicketDto dto) {
        Ticket created = ticketService.create(toEntity(dto), dto.getPerformanceId(), dto.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(created));
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PatchMapping("/{id}")
    public ResponseEntity<TicketDto> update(@PathVariable Long id, @RequestBody TicketDto dto) {
        return ticketService.update(id, toEntity(dto), dto.getPerformanceId(), dto.getUserId())
                .map(TicketController::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return ticketService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/purchase")
    public ResponseEntity<TicketDto> purchase(@RequestBody BuyTicketRequest request) {
        User currentUser = currentUserService.requireCurrentUser();
        Long userId;
        if (currentUserService.isStaff()) {
            userId = request.getUserId() != null ? request.getUserId() : currentUser.getId();
        } else {
            if (request.getUserId() != null && !currentUser.getId().equals(request.getUserId())) {
                throw new AccessDeniedException("Нельзя купить билет на другого пользователя");
            }
            userId = currentUser.getId();
        }
        Ticket ticket = ticketService.buyTicket(
                request.getPerformanceId(), userId, request.getSeatNumber());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(ticket));
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @ticketAccess.isOwner(#id, authentication.name)")
    @PostMapping("/{id}/refund")
    public ResponseEntity<TicketDto> refund(@PathVariable @P("id") Long id) {
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
