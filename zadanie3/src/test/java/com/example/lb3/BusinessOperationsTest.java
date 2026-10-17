package com.example.lb3;

import com.example.lb3.dto.PerformanceCancellationDto;
import com.example.lb3.entity.*;
import com.example.lb3.exception.BusinessRuleException;
import com.example.lb3.repository.*;
import com.example.lb3.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class BusinessOperationsTest {

    @Autowired private PlayService playService;
    @Autowired private PerformanceService performanceService;
    @Autowired private TicketService ticketService;
    @Autowired private TheatreHallService theatreHallService;

    @Autowired private PlayRepository playRepository;
    @Autowired private TheatreHallRepository hallRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PerformanceRepository performanceRepository;
    @Autowired private TicketRepository ticketRepository;

    private Play play(int duration) {
        Play p = new Play();
        p.setTitle("Тестовая пьеса");
        p.setDurationMinutes(duration);
        p.setAgeLimit(12);
        return playRepository.save(p);
    }

    private TheatreHall hall(String name, int capacity) {
        TheatreHall h = new TheatreHall();
        h.setName(name);
        h.setCapacity(capacity);
        return hallRepository.save(h);
    }

    private User user(String email) {
        User u = new User();
        u.setName("Зритель");
        u.setEmail(email);
        return userRepository.save(u);
    }

    private Performance performance(Play play, TheatreHall hall, LocalDateTime start, PerformanceStatus status) {
        Performance p = new Performance();
        p.setPlay(play);
        p.setHall(hall);
        p.setStartTime(start);
        p.setBasePrice(new BigDecimal("1000.00"));
        p.setStatus(status);
        return performanceRepository.save(p);
    }

    private Ticket ticket(Performance performance, User user, int seat, TicketStatus status) {
        Ticket t = new Ticket();
        t.setPerformance(performance);
        t.setUser(user);
        t.setSeatNumber(seat);
        t.setStatus(status);
        return ticketRepository.save(t);
    }

    @Test
    void buyTicket_sellsFreeSeat() {
        Performance perf = performance(play(120), hall("Большой", 50), LocalDateTime.now().plusDays(1),
                PerformanceStatus.SCHEDULED);
        User buyer = user("buyer@example.com");

        Ticket sold = ticketService.buyTicket(perf.getId(), buyer.getId(), 7);

        assertEquals(TicketStatus.SOLD, sold.getStatus());
        assertEquals(7, sold.getSeatNumber());
        assertEquals(buyer.getId(), sold.getUser().getId());
    }

    @Test
    void buyTicket_rejectsOccupiedSeat() {
        Performance perf = performance(play(120), hall("Большой", 50), LocalDateTime.now().plusDays(1),
                PerformanceStatus.SCHEDULED);
        User buyer = user("buyer@example.com");
        ticket(perf, buyer, 5, TicketStatus.SOLD);

        assertThrows(BusinessRuleException.class,
                () -> ticketService.buyTicket(perf.getId(), buyer.getId(), 5));
    }

    @Test
    void buyTicket_rejectsReservedSeat() {
        Performance perf = performance(play(120), hall("Большой", 50), LocalDateTime.now().plusDays(1),
                PerformanceStatus.SCHEDULED);
        User buyer = user("buyer@example.com");
        ticket(perf, null, 5, TicketStatus.RESERVED);

        assertThrows(BusinessRuleException.class,
                () -> ticketService.buyTicket(perf.getId(), buyer.getId(), 5));
    }

    @Test
    void buyTicket_rejectsCancelledPerformance() {
        Performance perf = performance(play(120), hall("Большой", 50), LocalDateTime.now().plusDays(1),
                PerformanceStatus.CANCELLED);
        User buyer = user("buyer@example.com");

        assertThrows(BusinessRuleException.class,
                () -> ticketService.buyTicket(perf.getId(), buyer.getId(), 1));
    }

    @Test
    void buyTicket_rejectsPastPerformance() {
        Performance perf = performance(play(120), hall("Большой", 50), LocalDateTime.now().minusDays(1),
                PerformanceStatus.COMPLETED);
        User buyer = user("buyer@example.com");

        assertThrows(BusinessRuleException.class,
                () -> ticketService.buyTicket(perf.getId(), buyer.getId(), 1));
    }

    @Test
    void buyTicket_rejectsSeatBeyondCapacity() {
        Performance perf = performance(play(120), hall("Малая", 10), LocalDateTime.now().plusDays(1),
                PerformanceStatus.SCHEDULED);
        User buyer = user("buyer@example.com");

        assertThrows(BusinessRuleException.class,
                () -> ticketService.buyTicket(perf.getId(), buyer.getId(), 11));
    }

    @Test
    void refundTicket_refundsSoldTicket() {
        Performance perf = performance(play(120), hall("Большой", 50), LocalDateTime.now().plusDays(1),
                PerformanceStatus.SCHEDULED);
        User buyer = user("buyer@example.com");
        Ticket sold = ticket(perf, buyer, 3, TicketStatus.SOLD);

        Ticket refunded = ticketService.refundTicket(sold.getId());

        assertEquals(TicketStatus.REFUNDED, refunded.getStatus());
    }

    @Test
    void refundTicket_rejectsNonSoldTicket() {
        Performance perf = performance(play(120), hall("Большой", 50), LocalDateTime.now().plusDays(1),
                PerformanceStatus.SCHEDULED);
        Ticket reserved = ticket(perf, null, 3, TicketStatus.RESERVED);

        assertThrows(BusinessRuleException.class, () -> ticketService.refundTicket(reserved.getId()));
    }

    @Test
    void refundTicket_rejectsAfterStart() {
        Performance perf = performance(play(120), hall("Большой", 50), LocalDateTime.now().minusHours(1),
                PerformanceStatus.COMPLETED);
        User buyer = user("buyer@example.com");
        Ticket sold = ticket(perf, buyer, 3, TicketStatus.SOLD);

        assertThrows(BusinessRuleException.class, () -> ticketService.refundTicket(sold.getId()));
    }

    @Test
    void cancelPerformance_cancelsPerformanceAndHandlesTickets() {
        Performance perf = performance(play(120), hall("Большой", 50), LocalDateTime.now().plusDays(1),
                PerformanceStatus.SCHEDULED);
        User buyer = user("buyer@example.com");
        Ticket sold = ticket(perf, buyer, 1, TicketStatus.SOLD);
        Ticket reserved = ticket(perf, null, 2, TicketStatus.RESERVED);

        PerformanceCancellationDto result = performanceService.cancelPerformance(perf.getId());

        assertEquals("CANCELLED", result.getStatus());
        assertEquals(1, result.getRefundedTickets());
        assertEquals(1, result.getCancelledTickets());
        assertEquals(PerformanceStatus.CANCELLED,
                performanceRepository.findById(perf.getId()).orElseThrow().getStatus());
        assertEquals(TicketStatus.REFUNDED,
                ticketRepository.findById(sold.getId()).orElseThrow().getStatus());
        assertEquals(TicketStatus.CANCELLED,
                ticketRepository.findById(reserved.getId()).orElseThrow().getStatus());
    }

    @Test
    void cancelPerformance_rejectsSecondCancellation() {
        Performance perf = performance(play(120), hall("Большой", 50), LocalDateTime.now().plusDays(1),
                PerformanceStatus.SCHEDULED);
        performanceService.cancelPerformance(perf.getId());

        assertThrows(BusinessRuleException.class, () -> performanceService.cancelPerformance(perf.getId()));
    }

    @Test
    void cancelPerformance_rejectsStartedPerformance() {
        Performance perf = performance(play(120), hall("Большой", 50), LocalDateTime.now().minusHours(1),
                PerformanceStatus.SCHEDULED);

        assertThrows(BusinessRuleException.class, () -> performanceService.cancelPerformance(perf.getId()));
    }

    @Test
    void reschedule_movesToBiggerHallAndNewTime() {
        Performance perf = performance(play(120), hall("Малая", 10), LocalDateTime.now().plusDays(1),
                PerformanceStatus.SCHEDULED);
        TheatreHall bigHall = hall("Большой", 100);
        User buyer = user("buyer@example.com");
        ticket(perf, buyer, 8, TicketStatus.SOLD);

        LocalDateTime newStart = LocalDateTime.now().plusDays(3);
        Performance rescheduled = performanceService.reschedulePerformance(
                perf.getId(), bigHall.getId(), newStart, new BigDecimal("1500.00"));

        assertEquals(bigHall.getId(), rescheduled.getHall().getId());
        assertEquals(newStart, rescheduled.getStartTime());
        assertEquals(0, new BigDecimal("1500.00").compareTo(rescheduled.getBasePrice()));
    }

    @Test
    void reschedule_rejectsHallTooSmallForSoldSeats() {
        Performance perf = performance(play(120), hall("Большой", 100), LocalDateTime.now().plusDays(1),
                PerformanceStatus.SCHEDULED);
        TheatreHall smallHall = hall("Малая", 10);
        User buyer = user("buyer@example.com");
        ticket(perf, buyer, 42, TicketStatus.SOLD);

        assertThrows(BusinessRuleException.class, () -> performanceService.reschedulePerformance(
                perf.getId(), smallHall.getId(), LocalDateTime.now().plusDays(3), null));
    }

    @Test
    void reschedule_rejectsTimeConflictInTargetHall() {
        TheatreHall hall = hall("Большой", 100);
        Play thePlay = play(120);
        LocalDateTime busyStart = LocalDateTime.now().plusDays(2);
        performance(thePlay, hall, busyStart, PerformanceStatus.SCHEDULED);

        Performance toMove = performance(play(90), hall("Малая", 50), LocalDateTime.now().plusDays(1),
                PerformanceStatus.SCHEDULED);

        assertThrows(BusinessRuleException.class, () -> performanceService.reschedulePerformance(
                toMove.getId(), hall.getId(), busyStart.plusMinutes(30), null));
    }

    @Test
    void archivePlay_archivesWhenNoFuturePerformances() {
        Play thePlay = play(120);

        Play archived = playService.archivePlay(thePlay.getId());

        assertTrue(archived.isArchived());
    }

    @Test
    void archivePlay_rejectsWhenFuturePerformanceExists() {
        Play thePlay = play(120);
        performance(thePlay, hall("Большой", 50), LocalDateTime.now().plusDays(5), PerformanceStatus.SCHEDULED);

        assertThrows(BusinessRuleException.class, () -> playService.archivePlay(thePlay.getId()));
    }

    @Test
    void archivePlay_rejectsAlreadyArchived() {
        Play thePlay = play(120);
        playService.archivePlay(thePlay.getId());

        assertThrows(BusinessRuleException.class, () -> playService.archivePlay(thePlay.getId()));
    }

    @Test
    void update_performance_rejectsStatusChangeToCancelled() {
        Performance perf = performance(play(120), hall("Большой", 50), LocalDateTime.now().plusDays(1),
                PerformanceStatus.SCHEDULED);
        Performance changes = new Performance();
        changes.setStatus(PerformanceStatus.CANCELLED);

        assertThrows(BusinessRuleException.class,
                () -> performanceService.update(perf.getId(), changes, null, null));
        assertEquals(PerformanceStatus.SCHEDULED,
                performanceRepository.findById(perf.getId()).orElseThrow().getStatus());
    }

    @Test
    void update_performance_completesAfterStartAndCancelsReservations() {
        Performance perf = performance(play(120), hall("Большой", 50), LocalDateTime.now().minusHours(3),
                PerformanceStatus.SCHEDULED);
        User buyer = user("buyer@example.com");
        Ticket sold = ticket(perf, buyer, 1, TicketStatus.SOLD);
        Ticket reserved = ticket(perf, null, 2, TicketStatus.RESERVED);
        Performance changes = new Performance();
        changes.setStatus(PerformanceStatus.COMPLETED);

        performanceService.update(perf.getId(), changes, null, null);

        assertEquals(PerformanceStatus.COMPLETED,
                performanceRepository.findById(perf.getId()).orElseThrow().getStatus());
        assertEquals(TicketStatus.SOLD, ticketRepository.findById(sold.getId()).orElseThrow().getStatus());
        assertEquals(TicketStatus.CANCELLED, ticketRepository.findById(reserved.getId()).orElseThrow().getStatus());
    }

    @Test
    void update_performance_rejectsCompletionBeforeStart() {
        Performance perf = performance(play(120), hall("Большой", 50), LocalDateTime.now().plusDays(1),
                PerformanceStatus.SCHEDULED);
        Performance changes = new Performance();
        changes.setStatus(PerformanceStatus.COMPLETED);

        assertThrows(BusinessRuleException.class,
                () -> performanceService.update(perf.getId(), changes, null, null));
    }

    @Test
    void update_performance_rejectsMoveIntoBusyHall() {
        TheatreHall busyHall = hall("Большой", 100);
        LocalDateTime busyStart = LocalDateTime.now().plusDays(2);
        performance(play(120), busyHall, busyStart, PerformanceStatus.SCHEDULED);
        Performance toMove = performance(play(90), hall("Малая", 50), LocalDateTime.now().plusDays(1),
                PerformanceStatus.SCHEDULED);
        Performance changes = new Performance();
        changes.setStartTime(busyStart.plusMinutes(30));

        assertThrows(BusinessRuleException.class,
                () -> performanceService.update(toMove.getId(), changes, null, busyHall.getId()));
    }

    @Test
    void update_performance_movesToFreeHall() {
        Performance perf = performance(play(120), hall("Малая", 10), LocalDateTime.now().plusDays(1),
                PerformanceStatus.SCHEDULED);
        TheatreHall freeHall = hall("Большой", 100);
        User buyer = user("buyer@example.com");
        ticket(perf, buyer, 8, TicketStatus.SOLD);
        Performance changes = new Performance();
        LocalDateTime newStart = LocalDateTime.now().plusDays(3);
        changes.setStartTime(newStart);

        Performance updated = performanceService.update(perf.getId(), changes, null, freeHall.getId())
                .orElseThrow();

        assertEquals(freeHall.getId(), updated.getHall().getId());
        assertEquals(newStart, updated.getStartTime());
    }

    @Test
    void update_performance_rejectsHallWithTooFewSeats() {
        Performance perf = performance(play(120), hall("Большой", 100), LocalDateTime.now().plusDays(1),
                PerformanceStatus.SCHEDULED);
        TheatreHall smallHall = hall("Малая", 10);
        User buyer = user("buyer@example.com");
        ticket(perf, buyer, 42, TicketStatus.SOLD);

        assertThrows(BusinessRuleException.class, () -> performanceService.update(
                perf.getId(), new Performance(), null, smallHall.getId()));
    }

    @Test
    void update_play_rejectsArchiveFlag() {
        Play thePlay = play(120);
        Play changes = new Play();
        changes.setArchived(true);

        assertThrows(BusinessRuleException.class, () -> playService.update(thePlay.getId(), changes));
        assertFalse(playRepository.findById(thePlay.getId()).orElseThrow().isArchived());
    }

    @Test
    void update_ticket_rejectsStatusChange() {
        Performance perf = performance(play(120), hall("Большой", 50), LocalDateTime.now().plusDays(1),
                PerformanceStatus.SCHEDULED);
        User buyer = user("buyer@example.com");
        Ticket sold = ticket(perf, buyer, 3, TicketStatus.SOLD);
        Ticket changes = new Ticket();
        changes.setStatus(TicketStatus.REFUNDED);

        assertThrows(BusinessRuleException.class,
                () -> ticketService.update(sold.getId(), changes, null, null));
        assertEquals(TicketStatus.SOLD, ticketRepository.findById(sold.getId()).orElseThrow().getStatus());
    }

    @Test
    void update_hall_rejectsCapacityBelowSoldSeats() {
        TheatreHall bigHall = hall("Большой", 100);
        Performance perf = performance(play(120), bigHall, LocalDateTime.now().plusDays(1),
                PerformanceStatus.SCHEDULED);
        User buyer = user("buyer@example.com");
        ticket(perf, buyer, 42, TicketStatus.SOLD);
        TheatreHall changes = new TheatreHall();
        changes.setCapacity(10);

        assertThrows(BusinessRuleException.class, () -> theatreHallService.update(bigHall.getId(), changes));
        assertEquals(100, hallRepository.findById(bigHall.getId()).orElseThrow().getCapacity());
    }

    @Test
    void create_performance_rejectsTimeConflictInHall() {
        TheatreHall theHall = hall("Большой", 100);
        LocalDateTime busyStart = LocalDateTime.now().plusDays(2);
        performance(play(120), theHall, busyStart, PerformanceStatus.SCHEDULED);
        Play newPlay = play(90);
        Performance toCreate = new Performance();
        toCreate.setStartTime(busyStart.plusMinutes(30));
        toCreate.setBasePrice(new BigDecimal("1000.00"));

        assertThrows(BusinessRuleException.class,
                () -> performanceService.create(toCreate, newPlay.getId(), theHall.getId()));
    }

    @Test
    void delete_performance_rejectsSoldTickets() {
        Performance perf = performance(play(120), hall("Большой", 50), LocalDateTime.now().plusDays(1),
                PerformanceStatus.SCHEDULED);
        User buyer = user("buyer@example.com");
        ticket(perf, buyer, 3, TicketStatus.SOLD);

        assertThrows(BusinessRuleException.class, () -> performanceService.delete(perf.getId()));
        assertTrue(performanceRepository.existsById(perf.getId()));
    }

    @Test
    void delete_performance_removesRefundedTickets() {
        Performance perf = performance(play(120), hall("Большой", 50), LocalDateTime.now().plusDays(1),
                PerformanceStatus.SCHEDULED);
        User buyer = user("buyer@example.com");
        Ticket sold = ticket(perf, buyer, 3, TicketStatus.SOLD);
        ticketService.refundTicket(sold.getId());

        assertTrue(performanceService.delete(perf.getId()));
        assertFalse(performanceRepository.existsById(perf.getId()));
        assertTrue(ticketRepository.findByPerformanceId(perf.getId()).isEmpty());
    }

    @Test
    void delete_play_rejectsSoldTickets() {
        Play thePlay = play(120);
        Performance perf = performance(thePlay, hall("Большой", 50), LocalDateTime.now().plusDays(1),
                PerformanceStatus.SCHEDULED);
        User buyer = user("buyer@example.com");
        ticket(perf, buyer, 3, TicketStatus.SOLD);

        assertThrows(BusinessRuleException.class, () -> playService.delete(thePlay.getId()));
        assertTrue(playRepository.existsById(thePlay.getId()));
    }

    @Test
    void delete_ticket_rejectsSoldTicket() {
        Performance perf = performance(play(120), hall("Большой", 50), LocalDateTime.now().plusDays(1),
                PerformanceStatus.SCHEDULED);
        User buyer = user("buyer@example.com");
        Ticket sold = ticket(perf, buyer, 3, TicketStatus.SOLD);

        assertThrows(BusinessRuleException.class, () -> ticketService.delete(sold.getId()));
        assertTrue(ticketRepository.existsById(sold.getId()));
    }
}
