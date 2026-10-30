package com.taskmanager.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "users", indexes = @Index(name = "idx_users_role", columnList = "role"))
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotBlank @Column(nullable = false, length = 100) private String name;
    @Email @NotBlank @Column(nullable = false, unique = true, length = 255) private String email;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Role role;
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setRole(Role role) { this.role = role; }
}
