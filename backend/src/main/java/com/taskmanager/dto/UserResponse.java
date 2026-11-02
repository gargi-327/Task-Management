package com.taskmanager.dto;

import com.taskmanager.entity.Role;
import com.taskmanager.entity.User;

public record UserResponse(Long id, String name, String email, Role role) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
