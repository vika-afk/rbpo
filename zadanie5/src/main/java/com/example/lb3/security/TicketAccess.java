package com.example.lb3.security;

import com.example.lb3.entity.Ticket;
import com.example.lb3.repository.TicketRepository;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component("ticketAccess")
public class TicketAccess {

    private final TicketRepository ticketRepository;

    public TicketAccess(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public boolean isOwner(Long ticketId, String email) {
        if (ticketId == null || email == null) {
            return false;
        }
        return ticketRepository.findById(ticketId)
                .map(Ticket::getUser)
                .map(user -> user.getEmail().equalsIgnoreCase(email.trim()))
                .orElse(false);
    }

    public void requireOwnerOrStaff(Long ticketId, CurrentUserService currentUserService) {
        if (currentUserService.isStaff()) {
            return;
        }
        String email = currentUserService.currentEmail().toLowerCase(Locale.ROOT);
        if (!isOwner(ticketId, email)) {
            throw new org.springframework.security.access.AccessDeniedException("Нет доступа к билету");
        }
    }
}
