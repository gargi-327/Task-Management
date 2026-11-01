package com.taskmanager.service;

import com.taskmanager.dto.ProjectRequest;
import com.taskmanager.dto.ProjectResponse;
import com.taskmanager.dto.ProjectSummaryResponse;
import com.taskmanager.dto.ProjectUpdateRequest;
import com.taskmanager.entity.Project;
import com.taskmanager.entity.TaskStatus;
import com.taskmanager.entity.User;
import com.taskmanager.exception.BadRequestException;
import com.taskmanager.exception.NotFoundException;
import com.taskmanager.repository.ProjectRepository;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.time.LocalDate;

@Service
@Transactional
public class ProjectService {
    private final ProjectRepository projects;
    private final UserRepository users;
    private final TaskRepository tasks;
    public ProjectService(ProjectRepository projects, UserRepository users, TaskRepository tasks) {
        this.projects = projects;
        this.users = users;
        this.tasks = tasks;
    }

    public ProjectResponse create(ProjectRequest request) {
        Project project = new Project();
        apply(project, request, true);
        return ProjectResponse.from(projects.save(project));
    }
    @Transactional(readOnly = true)
    public List<ProjectResponse> findAll(boolean includeArchived) {
        List<Project> results = includeArchived ? projects.findAll() : projects.findByArchivedFalseOrderByIdDesc();
        return results.stream().map(ProjectResponse::from).toList();
    }
    @Transactional(readOnly = true)
    public ProjectResponse findOne(Long id) { return ProjectResponse.from(getProject(id)); }
    public ProjectResponse update(Long id, ProjectUpdateRequest request) {
        Project project = getProject(id);
        applyUpdate(project, request);
        return ProjectResponse.from(project);
    }
    public ProjectResponse archive(Long id) {
        Project project = getProject(id);
        project.setArchived(true);
        return ProjectResponse.from(project);
    }
    @Transactional(readOnly = true)
    public ProjectSummaryResponse summary(Long id) {
        getProject(id); // A summary is only meaningful for a valid project.
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
    private Project getProject(Long id) {
        return projects.findById(id).orElseThrow(() -> new NotFoundException("Project " + id + " was not found"));
    }
    private void apply(Project project, ProjectRequest request, boolean creating) {
        if (request.name() == null || request.name().isBlank()) throw new BadRequestException("name must not be blank");
        if (request.createdById() == null) throw new BadRequestException("createdById is required");
        validateDateRange(request.startDate(), request.dueDate());
        User creator = users.findById(request.createdById()).orElseThrow(() -> new NotFoundException("User " + request.createdById() + " was not found"));
        project.setName(request.name().trim());
        project.setDescription(request.description());
        project.setStartDate(request.startDate());
        project.setDueDate(request.dueDate());
        if (creating) project.setCreatedBy(creator);
    }
    private void applyUpdate(Project project, ProjectUpdateRequest request) {
        if (request.name() != null) {
            if (request.name().isBlank()) throw new BadRequestException("name must not be blank");
            project.setName(request.name().trim());
        }
        if (request.description() != null) project.setDescription(request.description());

        LocalDate effectiveStartDate = request.startDate() != null ? request.startDate() : project.getStartDate();
        LocalDate effectiveDueDate = request.dueDate() != null ? request.dueDate() : project.getDueDate();
        validateDateRange(effectiveStartDate, effectiveDueDate);

        if (request.startDate() != null) project.setStartDate(request.startDate());
        if (request.dueDate() != null) project.setDueDate(request.dueDate());
    }
    private void validateDateRange(LocalDate startDate, LocalDate dueDate) {
        if (startDate != null && dueDate != null && dueDate.isBefore(startDate)) {
            throw new BadRequestException("dueDate cannot be before startDate");
        }
    }
}