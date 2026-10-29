package com.example.lb3.controller;

import com.example.lb3.dto.RegisterRequest;
import com.example.lb3.dto.UserDto;
import com.example.lb3.entity.User;
import com.example.lb3.security.CurrentUserService;
import com.example.lb3.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final CurrentUserService currentUserService;

    public AuthController(AuthService authService, CurrentUserService currentUserService) {
        this.authService = authService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> csrf(HttpServletRequest request) {
        CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        Map<String, String> map = new HashMap<>();
        if (csrf != null) {
            map.put("token", csrf.getToken());
            map.put("headerName", csrf.getHeaderName());
            map.put("parameterName", csrf.getParameterName());
        }
        return ResponseEntity.ok(map);
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request.getName(), request.getEmail(), request.getPassword());
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("role", user.getRole());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me() {
        User user = currentUserService.requireCurrentUser();
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setEnabled(user.getEnabled());
        dto.setBlocked(user.getBlocked());
        return ResponseEntity.ok(dto);
    }
}
