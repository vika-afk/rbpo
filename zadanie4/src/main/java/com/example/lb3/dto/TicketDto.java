package com.example.lb3.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TicketDto {
    private Long id;
    private Long performanceId;
    private Integer seatNumber;
    private Long userId;
    private String status;
}
