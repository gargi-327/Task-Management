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
@CrossOrigin(
        origins = "${app.cors.allowed-origin:http://localhost:5500}",
        allowedHeaders = {"Content-Type", "X-User-Id"},
        methods = {
                RequestMethod.GET,
                RequestMethod.POST,
                RequestMethod.PUT,
                RequestMethod.PATCH,
                RequestMethod.DELETE
        }
)
public class ProjectController {

    private final ProjectService service;

    public ProjectController(ProjectService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @Valid @RequestBody ProjectRequest request) {
        return service.create(request, userIdHeader);
    }

    @GetMapping
    public List<ProjectResponse> findAll(
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @RequestParam(defaultValue = "false") boolean includeArchived) {
        return service.findAll(userIdHeader, includeArchived);
    }

    @GetMapping("/{id}")
    public ProjectResponse findOne(
            @PathVariable Long id,
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader) {
        return service.findOne(id, userIdHeader);
    }

    @GetMapping("/{id}/summary")
    public ProjectSummaryResponse summary(
            @PathVariable Long id,
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader) {
        return service.summary(id, userIdHeader);
    }

    // Full project update
    @PutMapping("/{id}")
    public ProjectResponse update(
            @PathVariable Long id,
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @Valid @RequestBody ProjectRequest request) {
        return service.update(id, request, userIdHeader);
    }

    // Partial project update
    @PatchMapping("/{id}")
    public ProjectResponse partialUpdate(
            @PathVariable Long id,
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @Valid @RequestBody ProjectUpdateRequest request) {
        return service.update(id, request, userIdHeader);
    }

    @PatchMapping("/{id}/archive")
    public ProjectResponse archive(
            @PathVariable Long id,
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader) {
        return service.archive(id, userIdHeader);
    }
}