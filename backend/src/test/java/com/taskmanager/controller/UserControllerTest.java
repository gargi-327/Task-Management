package com.taskmanager.controller;

import com.taskmanager.dto.UserResponse;
import com.taskmanager.entity.Role;
import com.taskmanager.entity.User;
import com.taskmanager.exception.ConflictException;
import com.taskmanager.exception.ForbiddenException;
import com.taskmanager.exception.NotFoundException;
import com.taskmanager.repository.UserRepository;
import com.taskmanager.service.RequesterResolver;
import com.taskmanager.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(RequesterResolver.class)
class UserControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private UserService service;
    @MockBean private UserRepository users;

    @Test
    void createsUserWithCreatedStatusForGlobalManager() throws Exception {
        User requester = createUser(Role.MANAGER);
        when(users.findById(7L)).thenReturn(Optional.of(requester));
        when(service.create(any(), any(User.class))).thenReturn(
                new UserResponse(4L, "Avery", "avery@example.com", Role.MEMBER));

        mockMvc.perform(post("/api/users")
                        .header("X-User-Id", "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Avery\",\"email\":\"avery@example.com\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.role").value("MEMBER"));
    }

    @Test
    void rejectsInvalidRequestFields() throws Exception {
        mockMvc.perform(post("/api/users")
                        .header("X-User-Id", "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"invalid\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void rejectsUnknownRole() throws Exception {
        mockMvc.perform(post("/api/users")
                        .header("X-User-Id", "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Avery\",\"email\":\"avery@example.com\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsConflictForDuplicateEmail() throws Exception {
        when(users.findById(7L)).thenReturn(Optional.of(createUser(Role.MANAGER)));
        when(service.create(any(), any(User.class)))
                .thenThrow(new ConflictException("A user with email avery@example.com already exists"));

        mockMvc.perform(post("/api/users")
                        .header("X-User-Id", "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Avery\",\"email\":\"avery@example.com\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void rejectsMissingRequesterHeader() throws Exception {
        assertUnauthorized(post("/api/users"));
    }

    @Test
    void rejectsBlankRequesterHeader() throws Exception {
        assertUnauthorized(post("/api/users").header("X-User-Id", " "));
    }

    @Test
    void rejectsNonNumericRequesterHeader() throws Exception {
        assertUnauthorized(post("/api/users").header("X-User-Id", "manager"));
    }

    @Test
    void rejectsZeroRequesterId() throws Exception {
        assertUnauthorized(post("/api/users").header("X-User-Id", "0"));
    }

    @Test
    void rejectsNegativeRequesterId() throws Exception {
        assertUnauthorized(post("/api/users").header("X-User-Id", "-7"));
    }

    @Test
    void rejectsNonexistentRequesterId() throws Exception {
        when(users.findById(404L)).thenReturn(Optional.empty());

        assertUnauthorized(post("/api/users").header("X-User-Id", "404"));
    }

    @Test
    void rejectsGlobalMemberRequester() throws Exception {
        when(users.findById(7L)).thenReturn(Optional.of(createUser(Role.MEMBER)));
        when(service.create(any(), any(User.class)))
                .thenThrow(new ForbiddenException("Only global MANAGER users can create users"));

        mockMvc.perform(post("/api/users")
                        .header("X-User-Id", "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Avery\",\"email\":\"avery@example.com\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void listsUsers() throws Exception {
        when(service.findAll()).thenReturn(List.of(
                new UserResponse(4L, "Avery", "avery@example.com", Role.MEMBER)));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(4))
                .andExpect(jsonPath("$[0].name").value("Avery"));
    }

    @Test
    void retrievesUserById() throws Exception {
        when(service.findById(4L)).thenReturn(
                new UserResponse(4L, "Avery", "avery@example.com", Role.MANAGER));

        mockMvc.perform(get("/api/users/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("avery@example.com"));
    }

    @Test
    void returnsNotFoundForUnknownUser() throws Exception {
        when(service.findById(404L)).thenThrow(new NotFoundException("User 404 was not found"));

        mockMvc.perform(get("/api/users/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User 404 was not found"));
    }

    private void assertUnauthorized(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Avery\",\"email\":\"avery@example.com\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("A valid X-User-Id header is required"));
        verifyNoInteractions(service);
    }

    private User createUser(Role role) {
        User user = new User();
        user.setName("Requester");
        user.setEmail("requester@example.com");
        user.setRole(role);
        return user;
    }
}
