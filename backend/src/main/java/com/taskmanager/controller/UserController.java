package com.taskmanager.controller;

import com.taskmanager.dto.UserRequest;
import com.taskmanager.dto.UserResponse;
import com.taskmanager.entity.User;
import com.taskmanager.service.RequesterResolver;
import com.taskmanager.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "${app.cors.allowed-origin:http://localhost:5500}")
public class UserController {
    private final UserService service;
    private final RequesterResolver requesterResolver;

    public UserController(UserService service, RequesterResolver requesterResolver) {
        this.service = service;
        this.requesterResolver = requesterResolver;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @Valid @RequestBody UserRequest request) {
        User requester = requesterResolver.resolve(userIdHeader);
        return service.create(request, requester);
    }

    @GetMapping
    public List<UserResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public UserResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }
}
