package com.example.lb3.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiError {
    private int status;
    private String error;
    private String message;
}
