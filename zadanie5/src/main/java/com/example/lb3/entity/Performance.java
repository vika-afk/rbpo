package com.example.lb3.entity;

import javax.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "performances")
@Getter
@Setter
@NoArgsConstructor
public class Performance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "play_id", nullable = false)
    private Play play;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "hall_id", nullable = false)
    private TheatreHall hall;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "base_price", nullable = false)
    private BigDecimal basePrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PerformanceStatus status;
}
