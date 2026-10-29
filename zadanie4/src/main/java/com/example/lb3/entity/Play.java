package com.example.lb3.entity;

import javax.persistence.*;
import lombok.*;

@Entity
@Table(name = "plays")
@Getter
@Setter
@NoArgsConstructor
public class Play {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "age_limit", nullable = false)
    private Integer ageLimit;

    @Column(nullable = false)
    private boolean archived = false;
}
