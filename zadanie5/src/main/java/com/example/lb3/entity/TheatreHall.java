package com.example.lb3.entity;

import javax.persistence.*;
import lombok.*;

@Entity
@Table(name = "theatre_halls", uniqueConstraints = @UniqueConstraint(name = "uq_hall_name", columnNames = "name"))
@Getter
@Setter
@NoArgsConstructor
public class TheatreHall {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private Integer capacity;
}
