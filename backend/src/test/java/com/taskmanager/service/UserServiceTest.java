package com.taskmanager.service;

import com.taskmanager.dto.UserRequest;
import com.taskmanager.dto.UserResponse;
import com.taskmanager.entity.Role;
import com.taskmanager.entity.User;
import com.taskmanager.exception.BadRequestException;
import com.taskmanager.exception.ConflictException;
import com.taskmanager.exception.NotFoundException;
import com.taskmanager.repository.UserRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock private UserRepository users;
    @InjectMocks private UserService service;

    @Test
    void createsUser() {
        User savedUser = createUser(4L, "Avery", "avery@example.com", Role.MEMBER);
        when(users.existsByEmailIgnoreCase("avery@example.com")).thenReturn(false);
        when(users.saveAndFlush(any(User.class))).thenReturn(savedUser);

        UserResponse result = service.create(
                new UserRequest("Avery", "avery@example.com", Role.MEMBER), managerRequester());

        assertEquals(new UserResponse(4L, "Avery", "avery@example.com", Role.MEMBER), result);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(captor.capture());
        assertEquals("Avery", captor.getValue().getName());
        assertEquals("avery@example.com", captor.getValue().getEmail());
        assertEquals(Role.MEMBER, captor.getValue().getRole());
    }

    @Test
    void retrievesUserById() {
        User user = createUser(4L, "Avery", "avery@example.com", Role.MANAGER);
        when(users.findById(4L)).thenReturn(Optional.of(user));

        assertEquals(new UserResponse(4L, "Avery", "avery@example.com", Role.MANAGER), service.findById(4L));
    }

    @Test
    void listsUsers() {
        User manager = createUser(4L, "Avery", "avery@example.com", Role.MANAGER);
        User member = createUser(5L, "Sam", "sam@example.com", Role.MEMBER);
        when(users.findAll()).thenReturn(List.of(manager, member));

        assertEquals(List.of(
                new UserResponse(4L, "Avery", "avery@example.com", Role.MANAGER),
                new UserResponse(5L, "Sam", "sam@example.com", Role.MEMBER)), service.findAll());
    }

    @Test
    void rejectsDuplicateEmailIgnoringCase() {
        when(users.existsByEmailIgnoreCase("avery@example.com")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> service.create(new UserRequest("Avery", "avery@example.com", Role.MEMBER), managerRequester()));

        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void normalizesEmailBeforeSaving() {
        User savedUser = createUser(4L, "Avery", "avery@example.com", Role.MEMBER);
        when(users.existsByEmailIgnoreCase("avery@example.com")).thenReturn(false);
        when(users.saveAndFlush(any(User.class))).thenReturn(savedUser);

        service.create(new UserRequest("Avery", "Avery@Example.com", Role.MEMBER), managerRequester());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(captor.capture());
        assertEquals("avery@example.com", captor.getValue().getEmail());
    }

    @Test
    void translatesConcurrentEmailUniquenessViolation() {
        when(users.existsByEmailIgnoreCase("avery@example.com")).thenReturn(false);
        ConstraintViolationException constraintViolation =
                new ConstraintViolationException("duplicate", null, "insert into users", "uk_users_email");
        when(users.saveAndFlush(any(User.class))).thenThrow(
                new org.springframework.dao.DataIntegrityViolationException("duplicate", constraintViolation));

        assertThrows(ConflictException.class,
                () -> service.create(new UserRequest("Avery", "avery@example.com", Role.MEMBER), managerRequester()));
    }

    @Test
    void doesNotReportOtherIntegrityViolationsAsDuplicateEmail() {
        ConstraintViolationException constraintViolation =
                new ConstraintViolationException("other constraint", null, "insert into users", "some_other_constraint");
        when(users.existsByEmailIgnoreCase("avery@example.com")).thenReturn(false);
        when(users.saveAndFlush(any(User.class))).thenThrow(
                new org.springframework.dao.DataIntegrityViolationException("integrity failure", constraintViolation));

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> service.create(new UserRequest("Avery", "avery@example.com", Role.MEMBER), managerRequester()));
    }

    @Test
    void rejectsMissingRole() {
        assertThrows(BadRequestException.class,
                () -> service.create(new UserRequest("Avery", "avery@example.com", null), managerRequester()));

        verifyNoInteractions(users);
    }

    @Test
    void rejectsMemberRequester() {
        User member = new User();
        member.setName("Sam");
        member.setEmail("sam@example.com");
        member.setRole(Role.MEMBER);

        assertThrows(com.taskmanager.exception.ForbiddenException.class,
                () -> service.create(new UserRequest("Avery", "avery@example.com", Role.MANAGER), member));

        verifyNoInteractions(users);
    }

    @Test
    void rejectsNonexistentUserId() {
        when(users.findById(404L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.findById(404L));
    }

    @Test
    void rejectsInvalidUserId() {
        assertThrows(BadRequestException.class, () -> service.findById(0L));

        verifyNoInteractions(users);
    }

    private User createUser(Long id, String name, String email, Role role) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getName()).thenReturn(name);
        when(user.getEmail()).thenReturn(email);
        when(user.getRole()).thenReturn(role);
        return user;
    }

    private User managerRequester() {
        User manager = new User();
        manager.setName("Manager");
        manager.setEmail("manager@example.com");
        manager.setRole(Role.MANAGER);
        return manager;
    }
}
