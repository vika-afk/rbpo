package com.example.lb3.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class PerformanceDto {
    private Long id;
    private Long playId;
    private Long hallId;
    private LocalDateTime startTime;
    private BigDecimal basePrice;
    private String status;
}
