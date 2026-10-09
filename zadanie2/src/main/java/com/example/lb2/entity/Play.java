package com.example.lb2.entity;

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

    private String title;
    private Integer durationMinutes;
    private Integer ageLimit;
}