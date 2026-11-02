package com.taskmanager.service;

import com.taskmanager.dto.UserRequest;
import com.taskmanager.entity.Role;
import com.taskmanager.entity.User;
import com.taskmanager.repository.UserRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class InitialManagerBootstrap implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(InitialManagerBootstrap.class);

    private final UserRepository users;
    private final Validator validator;
    private final String configuredName;
    private final String configuredEmail;

    public InitialManagerBootstrap(
            UserRepository users,
            Validator validator,
            @Value("${app.bootstrap.manager.name:}") String configuredName,
            @Value("${app.bootstrap.manager.email:}") String configuredEmail) {
        this.users = users;
        this.validator = validator;
        this.configuredName = configuredName;
        this.configuredEmail = configuredEmail;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            logger.info("Initial MANAGER bootstrap skipped because users already exist");
            return;
        }

        if (isBlank(configuredName) && isBlank(configuredEmail)) {
            logger.warn("Initial MANAGER bootstrap skipped; configure "
                    + "app.bootstrap.manager.name and app.bootstrap.manager.email to enable it");
            return;
        }

        if (isBlank(configuredName) || isBlank(configuredEmail)) {
            throw new IllegalStateException(
                    "Initial MANAGER bootstrap requires both name and email configuration");
        }

        String name = configuredName.trim();
        String email = configuredEmail.trim().toLowerCase(Locale.ROOT);
        UserRequest validationRequest = new UserRequest(name, email, Role.MANAGER);
        Set<ConstraintViolation<UserRequest>> violations = validator.validate(validationRequest);
        if (!violations.isEmpty()) {
            String details = violations.stream()
                    .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                    .sorted()
                    .collect(Collectors.joining(", "));
            throw new IllegalStateException("Invalid initial MANAGER configuration: " + details);
        }

        User manager = new User();
        manager.setName(name);
        manager.setEmail(email);
        manager.setRole(Role.MANAGER);
        User savedManager = users.saveAndFlush(manager);
        logger.info("Created initial demo MANAGER account with user ID {}", savedManager.getId());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
