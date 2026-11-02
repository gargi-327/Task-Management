package com.taskmanager.service;

import com.taskmanager.dto.ProjectRequest;
import com.taskmanager.dto.ProjectResponse;
import com.taskmanager.dto.ProjectSummaryResponse;
import com.taskmanager.dto.ProjectUpdateRequest;
import com.taskmanager.entity.Project;
import com.taskmanager.entity.ProjectMember;
import com.taskmanager.entity.Role;
import com.taskmanager.entity.TaskStatus;
import com.taskmanager.entity.User;
import com.taskmanager.exception.BadRequestException;
import com.taskmanager.exception.ForbiddenException;
import com.taskmanager.exception.NotFoundException;
import com.taskmanager.repository.ProjectMemberRepository;
import com.taskmanager.repository.ProjectRepository;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projects;
    private final UserRepository users;
    private final TaskRepository tasks;
    private final ProjectMemberRepository memberships;
    private final RequesterResolver requesterResolver;

    public ProjectService(
            ProjectRepository projects,
            UserRepository users,
            TaskRepository tasks,
            ProjectMemberRepository memberships,
            RequesterResolver requesterResolver
    ) {
        this.projects = projects;
        this.users = users;
        this.tasks = tasks;
        this.memberships = memberships;
        this.requesterResolver = requesterResolver;
    }

    public ProjectResponse create(ProjectRequest request, String userIdHeader) {
        User requester = requesterResolver.resolve(userIdHeader);
        if (requester.getRole() != Role.MANAGER) {
            throw new ForbiddenException("Only global MANAGER users can create projects");
        }

        if (request == null) {
            throw new BadRequestException("Request body is required");
        }
        if (request.createdById() == null) {
            throw new BadRequestException("createdById is required");
        }
        if (!request.createdById().equals(requester.getId())) {
            throw new BadRequestException("createdById must match X-User-Id");
        }

        Project project = new Project();
        apply(project, request, requester, true);
        Project savedProject = projects.save(project);
        memberships.save(new ProjectMember(savedProject, requester, "MANAGER"));
        return ProjectResponse.from(savedProject);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> findAll(String userIdHeader, boolean includeArchived) {
        User requester = requesterResolver.resolve(userIdHeader);
        Set<Long> projectIds = memberships.findAllByUserId(requester.getId()).stream()
                .map(membership -> membership.getProject().getId())
                .collect(Collectors.toSet());
        List<Project> results = includeArchived
                ? projects.findAll()
                : projects.findByArchivedFalseOrderByIdDesc();

        return results.stream()
                .filter(project -> projectIds.contains(project.getId()))
                .map(ProjectResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse findOne(Long id, String userIdHeader) {
        User requester = requesterResolver.resolve(userIdHeader);
        Project project = requireMembership(id, requester);
        return ProjectResponse.from(project);
    }

    // Full update using ProjectRequest
    public ProjectResponse update(Long id, ProjectRequest request, String userIdHeader) {
        Project project = requireProjectManager(id, userIdHeader);
        if (request == null) {
            throw new BadRequestException("Request body is required");
        }
        apply(project, request, null, false);
        return ProjectResponse.from(project);
    }

    // Partial update using ProjectUpdateRequest
    public ProjectResponse update(Long id, ProjectUpdateRequest request, String userIdHeader) {
        Project project = requireProjectManager(id, userIdHeader);
        if (request == null) {
            throw new BadRequestException("Request body is required");
        }
        applyUpdate(project, request);
        return ProjectResponse.from(project);
    }

    public ProjectResponse archive(Long id, String userIdHeader) {
        Project project = requireProjectManager(id, userIdHeader);
        project.setArchived(true);
        return ProjectResponse.from(project);
    }

    @Transactional(readOnly = true)
    public ProjectSummaryResponse summary(Long id, String userIdHeader) {
        User requester = requesterResolver.resolve(userIdHeader);
        requireMembership(id, requester);

        LocalDate today = LocalDate.now();

        return new ProjectSummaryResponse(
                id,
                tasks.countByProjectId(id),
                tasks.countByProjectIdAndStatus(id, TaskStatus.TODO),
                tasks.countByProjectIdAndStatus(id, TaskStatus.IN_PROGRESS),
                tasks.countByProjectIdAndStatus(id, TaskStatus.DONE),
                tasks.countOverdueByProjectId(id, today),
                today
        );
    }

    private Project requireMembership(Long id, User requester) {
        validateProjectId(id);
        Project project = getProject(id);
        if (!memberships.existsByProjectIdAndUserId(id, requester.getId())) {
            throw new ForbiddenException("Project membership is required");
        }
        return project;
    }

    private Project requireProjectManager(Long id, String userIdHeader) {
        User requester = requesterResolver.resolve(userIdHeader);
        validateProjectId(id);
        Project project = getProject(id);
        ProjectMember membership = memberships.findByProjectIdAndUserId(id, requester.getId())
                .orElseThrow(() -> new ForbiddenException("Project MANAGER membership is required"));
        if (!"MANAGER".equals(membership.getMembershipRole())) {
            throw new ForbiddenException("Project MANAGER membership is required");
        }
        return project;
    }

    private void validateProjectId(Long id) {
        if (id == null || id <= 0) {
            throw new BadRequestException("projectId must be a positive number");
        }
    }

    private Project getProject(Long id) {
        return projects.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("Project " + id + " was not found"));
    }

    private void apply(
            Project project,
            ProjectRequest request,
            User creator,
            boolean creating
    ) {
        if (request.name() == null || request.name().isBlank()) {
            throw new BadRequestException("name must not be blank");
        }

        if (request.createdById() == null) {
            throw new BadRequestException("createdById is required");
        }

        validateDateRange(request.startDate(), request.dueDate());

        if (!creating) {
            users.findById(request.createdById())
                    .orElseThrow(() ->
                            new NotFoundException(
                                    "User " + request.createdById() + " was not found"
                            ));
        }

        project.setName(request.name().trim());
        project.setDescription(request.description());
        project.setStartDate(request.startDate());
        project.setDueDate(request.dueDate());

        if (creating) {
            project.setCreatedBy(creator);
        }
    }

    private void applyUpdate(Project project, ProjectUpdateRequest request) {

        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw new BadRequestException("name must not be blank");
            }

            project.setName(request.name().trim());
        }

        if (request.description() != null) {
            project.setDescription(request.description());
        }

        LocalDate effectiveStartDate =
                request.startDate() != null
                        ? request.startDate()
                        : project.getStartDate();

        LocalDate effectiveDueDate =
                request.dueDate() != null
                        ? request.dueDate()
                        : project.getDueDate();

        validateDateRange(effectiveStartDate, effectiveDueDate);

        if (request.startDate() != null) {
            project.setStartDate(request.startDate());
        }

        if (request.dueDate() != null) {
            project.setDueDate(request.dueDate());
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate dueDate) {
        if (startDate != null && dueDate != null
                && dueDate.isBefore(startDate)) {
            throw new BadRequestException(
                    "dueDate cannot be before startDate"
            );
        }
    }
}