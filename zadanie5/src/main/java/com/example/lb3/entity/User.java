package com.example.lb3.entity;

import javax.persistence.*;
import lombok.*;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uq_user_email", columnNames = "email"))
@Getter
@Setter
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role = "USER";

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(nullable = false)
    private Boolean blocked = false;
}
