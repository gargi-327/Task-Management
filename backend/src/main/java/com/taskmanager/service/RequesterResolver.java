package com.taskmanager.service;

import com.taskmanager.entity.User;
import com.taskmanager.exception.UnauthorizedException;
import com.taskmanager.repository.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class RequesterResolver {
    private static final String INVALID_REQUESTER_MESSAGE = "A valid X-User-Id header is required";

    private final UserRepository users;

    public RequesterResolver(UserRepository users) {
        this.users = users;
    }

    public User resolve(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw invalidRequester();
        }

        final long userId;
        try {
            userId = Long.parseLong(userIdHeader.trim());
        } catch (NumberFormatException ex) {
            throw invalidRequester();
        }

        if (userId <= 0) {
            throw invalidRequester();
        }

        return users.findById(userId).orElseThrow(this::invalidRequester);
    }

    private UnauthorizedException invalidRequester() {
        return new UnauthorizedException(INVALID_REQUESTER_MESSAGE);
    }
}
