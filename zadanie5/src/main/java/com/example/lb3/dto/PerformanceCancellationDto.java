package com.example.lb3.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PerformanceCancellationDto {
    private Long performanceId;
    private String status;
    private int refundedTickets;
    private int cancelledTickets;
}
