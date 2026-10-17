package com.example.lb3.repository;

import com.example.lb3.entity.Ticket;
import com.example.lb3.entity.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByPerformanceId(Long performanceId);

    List<Ticket> findByUserId(Long userId);

    List<Ticket> findByPerformanceIdAndStatusIn(Long performanceId, Collection<TicketStatus> statuses);

    boolean existsByPerformanceIdAndStatus(Long performanceId, TicketStatus status);

    boolean existsByPerformanceIdAndSeatNumberAndStatusIn(Long performanceId, Integer seatNumber,
                                                          Collection<TicketStatus> statuses);

    @Query("select max(t.seatNumber) from Ticket t "
            + "where t.performance.hall.id = :hallId and t.status in :statuses")
    Integer findMaxActiveSeatNumberInHall(@Param("hallId") Long hallId,
                                          @Param("statuses") Collection<TicketStatus> statuses);
}
