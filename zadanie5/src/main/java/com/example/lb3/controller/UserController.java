package com.example.lb3.controller;

import com.example.lb3.dto.UserCreateRequest;
import com.example.lb3.dto.UserDto;
import com.example.lb3.dto.UserUpdateRequest;
import com.example.lb3.entity.User;
import com.example.lb3.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<UserDto>> getAll() {
        List<UserDto> users = userService.findAll().stream()
                .map(UserController::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getById(@PathVariable Long id) {
        return userService.findById(id)
                .map(UserController::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<UserDto> create(@Valid @RequestBody UserCreateRequest request) {
        User created = userService.createUser(request.getName(), request.getEmail(), request.getPassword(),
                request.getRole(), request.getEnabled(), request.getBlocked());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(created));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserDto> update(@PathVariable Long id,
                                          @Valid @RequestBody UserUpdateRequest request) {
        return userService.updateUser(id, request.getName(), request.getEmail(), request.getPassword(),
                        request.getRole(), request.getEnabled(), request.getBlocked())
                .map(UserController::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return userService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    private static UserDto toDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setEnabled(user.getEnabled());
        dto.setBlocked(user.getBlocked());
        return dto;
    }
}
