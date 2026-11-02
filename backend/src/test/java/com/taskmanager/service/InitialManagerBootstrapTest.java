package com.taskmanager.service;

import com.taskmanager.entity.Role;
import com.taskmanager.entity.User;
import com.taskmanager.repository.UserRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InitialManagerBootstrapTest {
    @Mock private UserRepository users;

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void createsInitialManagerWhenDatabaseIsEmptyAndConfigurationIsValid() {
        when(users.count()).thenReturn(0L);
        when(users.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User manager = invocation.getArgument(0);
            manager.setName("Local Manager");
            return manager;
        });
        InitialManagerBootstrap bootstrap = bootstrap(" Local Manager ", "Manager@Example.test");

        bootstrap.run(new DefaultApplicationArguments(new String[0]));

        verify(users).saveAndFlush(argThat(manager ->
                "Local Manager".equals(manager.getName())
                        && "manager@example.test".equals(manager.getEmail())
                        && manager.getRole() == Role.MANAGER));
    }

    @Test
    void doesNotCreateManagerWhenAnyUserAlreadyExists() {
        when(users.count()).thenReturn(1L);
        InitialManagerBootstrap bootstrap = bootstrap("Local Manager", "manager@example.test");

        bootstrap.run(new DefaultApplicationArguments(new String[0]));

        verify(users, never()).saveAndFlush(any(User.class));
    }

    @Test
    void repeatedInvocationDoesNotCreateDuplicateManager() {
        AtomicLong userCount = new AtomicLong();
        when(users.count()).thenAnswer(invocation -> userCount.get());
        when(users.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            userCount.incrementAndGet();
            User manager = invocation.getArgument(0);
            manager.setName("Local Manager");
            return manager;
        });
        InitialManagerBootstrap bootstrap = bootstrap("Local Manager", "manager@example.test");

        bootstrap.run(new DefaultApplicationArguments(new String[0]));
        bootstrap.run(new DefaultApplicationArguments(new String[0]));

        verify(users, times(1)).saveAndFlush(any(User.class));
    }

    @Test
    void skipsBootstrapWhenConfigurationIsMissing() {
        when(users.count()).thenReturn(0L);
        InitialManagerBootstrap bootstrap = bootstrap("", "");

        assertDoesNotThrow(() ->
                bootstrap.run(new DefaultApplicationArguments(new String[0])));

        verify(users, never()).saveAndFlush(any(User.class));
    }

    @Test
    void rejectsPartiallyMissingConfiguration() {
        when(users.count()).thenReturn(0L);
        InitialManagerBootstrap bootstrap = bootstrap("Local Manager", "");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> bootstrap.run(new DefaultApplicationArguments(new String[0])));

        assertTrue(exception.getMessage().contains("requires both name and email"));
        verify(users, never()).saveAndFlush(any(User.class));
    }

    @Test
    void rejectsInvalidBootstrapDetailsClearly() {
        when(users.count()).thenReturn(0L);
        InitialManagerBootstrap bootstrap = bootstrap("A".repeat(101), "not-an-email");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> bootstrap.run(new DefaultApplicationArguments(new String[0])));

        assertTrue(exception.getMessage().contains("Invalid initial MANAGER configuration"));
        assertTrue(exception.getMessage().contains("email"));
        assertTrue(exception.getMessage().contains("name"));
        verify(users, never()).saveAndFlush(any(User.class));
    }

    private InitialManagerBootstrap bootstrap(String name, String email) {
        return new InitialManagerBootstrap(users, validator, name, email);
    }
}
