package com.taskmanager.controller;

import com.taskmanager.dto.ProjectRequest;
import com.taskmanager.dto.ProjectResponse;
import com.taskmanager.dto.ProjectSummaryResponse;
import com.taskmanager.dto.ProjectUpdateRequest;
import com.taskmanager.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService service;

    public ProjectController(ProjectService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(@Valid @RequestBody ProjectRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<ProjectResponse> findAll(
            @RequestParam(defaultValue = "false") boolean includeArchived) {
        return service.findAll(includeArchived);
    }

    @GetMapping("/{id}")
    public ProjectResponse findOne(@PathVariable Long id) {
        return service.findOne(id);
    }

    @GetMapping("/{id}/summary")
    public ProjectSummaryResponse summary(@PathVariable Long id) {
        return service.summary(id);
    }

    // Full project update
    @PutMapping("/{id}")
    public ProjectResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ProjectRequest request) {
        return service.update(id, request);
    }

    // Partial project update
    @PatchMapping("/{id}")
    public ProjectResponse partialUpdate(
            @PathVariable Long id,
            @Valid @RequestBody ProjectUpdateRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/archive")
    public ProjectResponse archive(@PathVariable Long id) {
        return service.archive(id);
    }
}