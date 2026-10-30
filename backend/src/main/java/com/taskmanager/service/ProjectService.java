package com.taskmanager.service;

import com.taskmanager.dto.ProjectRequest;
import com.taskmanager.dto.ProjectResponse;
import com.taskmanager.entity.Project;
import com.taskmanager.entity.User;
import com.taskmanager.exception.BadRequestException;
import com.taskmanager.exception.NotFoundException;
import com.taskmanager.repository.ProjectRepository;
import com.taskmanager.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class ProjectService {
    private final ProjectRepository projects;
    private final UserRepository users;
    public ProjectService(ProjectRepository projects, UserRepository users) { this.projects = projects; this.users = users; }

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
    public ProjectResponse update(Long id, ProjectRequest request) {
        Project project = getProject(id);
        apply(project, request, false);
        return ProjectResponse.from(project);
    }
    public ProjectResponse archive(Long id) {
        Project project = getProject(id);
        project.setArchived(true);
        return ProjectResponse.from(project);
    }
    private Project getProject(Long id) {
        return projects.findById(id).orElseThrow(() -> new NotFoundException("Project " + id + " was not found"));
    }
    private void apply(Project project, ProjectRequest request, boolean creating) {
        if (request.startDate() != null && request.dueDate() != null && request.dueDate().isBefore(request.startDate())) {
            throw new BadRequestException("dueDate cannot be before startDate");
        }
        User creator = users.findById(request.createdById()).orElseThrow(() -> new NotFoundException("User " + request.createdById() + " was not found"));
        project.setName(request.name().trim());
        project.setDescription(request.description());
        project.setStartDate(request.startDate());
        project.setDueDate(request.dueDate());
        if (creating) project.setCreatedBy(creator);
    }
}
