package com.example.lb2.entity;

import javax.persistence.*;
import lombok.*;

@Entity
@Table(name = "theatre_halls")
@Getter
@Setter
@NoArgsConstructor
public class TheatreHall {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Integer capacity;
}