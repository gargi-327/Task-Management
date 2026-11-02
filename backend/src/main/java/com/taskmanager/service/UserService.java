package com.taskmanager.service;

import com.taskmanager.dto.UserRequest;
import com.taskmanager.dto.UserResponse;
import com.taskmanager.entity.Role;
import com.taskmanager.entity.User;
import com.taskmanager.exception.BadRequestException;
import com.taskmanager.exception.ConflictException;
import com.taskmanager.exception.ForbiddenException;
import com.taskmanager.exception.NotFoundException;
import com.taskmanager.exception.UnauthorizedException;
import com.taskmanager.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class UserService {
    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
    }

    public UserResponse create(UserRequest request, User requester) {
        if (requester == null) {
            throw new UnauthorizedException("A valid X-User-Id header is required");
        }
        if (requester.getRole() != Role.MANAGER) {
            throw new ForbiddenException("Only global MANAGER users can create users");
        }
        if (request == null) throw new BadRequestException("Request body is required");
        if (request.role() == null) throw new BadRequestException("role is required");

        String email = request.email() == null ? null : request.email().trim().toLowerCase(Locale.ROOT);
        if (email != null && users.existsByEmailIgnoreCase(email)) {
            throw duplicateEmail(email);
        }

        User user = new User();
        user.setName(request.name() == null ? null : request.name().trim());
        user.setEmail(email);
        user.setRole(request.role());

        try {
            return UserResponse.from(users.saveAndFlush(user));
        } catch (DataIntegrityViolationException ex) {
            if (isEmailConstraintViolation(ex)) throw duplicateEmail(email);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        if (id == null || id <= 0) throw new BadRequestException("id must be a positive number");
        User user = users.findById(id)
                .orElseThrow(() -> new NotFoundException("User " + id + " was not found"));
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return users.findAll().stream().map(UserResponse::from).toList();
    }

    private ConflictException duplicateEmail(String email) {
        return new ConflictException("A user with email " + email + " already exists");
    }

    private boolean isEmailConstraintViolation(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                return "uk_users_email".equalsIgnoreCase(violation.getConstraintName());
            }
        }
        return false;
    }
}
