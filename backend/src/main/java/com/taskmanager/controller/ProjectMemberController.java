package com.taskmanager.controller;

import com.taskmanager.dto.ProjectMemberRequest;
import com.taskmanager.dto.ProjectMemberResponse;
import com.taskmanager.service.ProjectMemberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/members")
@CrossOrigin(origins = "${app.cors.allowed-origin:http://localhost:5500}")
public class ProjectMemberController {
    private final ProjectMemberService service;

    public ProjectMemberController(ProjectMemberService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectMemberResponse addMember(
            @PathVariable Long projectId,
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @Valid @RequestBody ProjectMemberRequest request) {
        return service.addMember(projectId, userIdHeader, request);
    }

    @GetMapping
    public List<ProjectMemberResponse> findMembers(
            @PathVariable Long projectId,
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader) {
        return service.findMembers(projectId, userIdHeader);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader) {
        service.removeMember(projectId, userId, userIdHeader);
    }
}
