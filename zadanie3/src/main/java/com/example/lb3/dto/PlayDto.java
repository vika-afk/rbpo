package com.example.lb3.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayDto {
    private Long id;
    private String title;
    private Integer durationMinutes;
    private Integer ageLimit;
    private boolean archived;
}
