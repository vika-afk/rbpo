package com.example.lb3.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BuyTicketRequest {
    private Long performanceId;
    private Long userId;
    private Integer seatNumber;
}
