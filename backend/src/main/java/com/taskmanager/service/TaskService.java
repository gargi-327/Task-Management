
package com.taskmanager.service;

import com.taskmanager.dto.TaskRequest;
import com.taskmanager.entity.*;
import com.taskmanager.exception.BadRequestException;
import com.taskmanager.exception.ForbiddenException;
import com.taskmanager.exception.NotFoundException;
import com.taskmanager.repository.ProjectMemberRepository;
import com.taskmanager.repository.ProjectRepository;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class TaskService {
    private static final int MAX_TASK_TITLE_LENGTH = 150;
    private static final int MAX_TASK_DESCRIPTION_BYTES = 65_535;
    private static final int MAX_TASK_DESCRIPTION_LENGTH = 65_535;

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final RequesterResolver requesterResolver;

    public TaskService(TaskRepository taskRepository,
                       ProjectRepository projectRepository,
                       UserRepository userRepository,
                       ProjectMemberRepository projectMemberRepository,
                       RequesterResolver requesterResolver) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.requesterResolver = requesterResolver;
    }

    // Create a task
    public Task createTask(TaskRequest request, String userIdHeader) {
        User requester = requesterResolver.resolve(userIdHeader);

        if (request == null) {
            throw new BadRequestException("Task request is required");
        }

        if (request.getTitle() == null ||
                request.getTitle().isBlank()) {
            throw new BadRequestException("Task title is required");
        }
        validateTitleLength(request.getTitle());

        if (request.getProjectId() == null) {
            throw new BadRequestException("Project ID is required");
        }
        validateId(request.getProjectId(), "projectId");

        if (request.getStartDate() == null) {
            throw new BadRequestException("Start date is required");
        }

        validateDescription(request.getDescription());
        if (request.getAssigneeId() != null) {
            validateId(request.getAssigneeId(), "assigneeId");
        }

        if (request.getDueDate() != null &&
                request.getDueDate().isBefore(request.getStartDate())) {
            throw new BadRequestException(
                    "Due date cannot be before start date");
        }

        Project project = requireProjectManager(request.getProjectId(), requester);

        Task task = new Task();

        task.setProject(project);
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority() != null
                ? request.getPriority() : TaskPriority.MEDIUM);
        task.setStatus(request.getStatus() != null
                ? request.getStatus() : TaskStatus.TODO);
        task.setStartDate(request.getStartDate());
        task.setDueDate(request.getDueDate());
        task.setCreatedBy(requester);

        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(
                            request.getAssigneeId())
                    .orElseThrow(() ->
                            new NotFoundException("Assignee not found"));

            requireProjectMember(project.getId(), assignee.getId());
            task.setAssignee(assignee);
        }

        return taskRepository.save(task);
    }

    // Get all tasks
    @Transactional(readOnly = true)
    public List<Task> getAllTasks(String userIdHeader, Long projectId) {
        User requester = requesterResolver.resolve(userIdHeader);
        if (projectId != null) {
            requireProjectMembership(projectId, requester);
            return visibleTasks(requester, taskRepository.findByProject_Id(projectId));
        }
        return visibleTasks(requester, taskRepository.findAll());
    }

    // Get task by ID
    @Transactional(readOnly = true)
    public Task getTaskById(Long id, String userIdHeader) {
        User requester = requesterResolver.resolve(userIdHeader);
        validateId(id, "taskId");
        Task task = taskRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("Task not found"));
        requireTaskVisibility(task, requester);
        return task;
    }

    // Get tasks by project
    @Transactional(readOnly = true)
    public List<Task> getTasksByProject(Long projectId, String userIdHeader) {
        User requester = requesterResolver.resolve(userIdHeader);
        requireProjectMembership(projectId, requester);
        return visibleTasks(requester, taskRepository.findByProject_Id(projectId));
    }

    // Get tasks by status
    @Transactional(readOnly = true)
    public List<Task> getTasksByStatus(TaskStatus status, String userIdHeader) {
        User requester = requesterResolver.resolve(userIdHeader);
        return visibleTasks(requester, taskRepository.findByStatus(status));
    }

    // Get tasks by priority
    @Transactional(readOnly = true)
    public List<Task> getTasksByPriority(TaskPriority priority, String userIdHeader) {
        User requester = requesterResolver.resolve(userIdHeader);
        return visibleTasks(requester, taskRepository.findByPriority(priority));
    }

    // Get tasks by due date
    @Transactional(readOnly = true)
    public List<Task> getTasksByDueDate(LocalDate dueDate, String userIdHeader) {
        User requester = requesterResolver.resolve(userIdHeader);
        return visibleTasks(requester, taskRepository.findByDueDate(dueDate));
    }

    // Get tasks assigned to a user
    @Transactional(readOnly = true)
    public List<Task> getTasksByAssignee(Long assigneeId, String userIdHeader) {
        User requester = requesterResolver.resolve(userIdHeader);
        return visibleTasks(requester, taskRepository.findByAssignee_Id(assigneeId));
    }

    // Get overdue tasks
    @Transactional(readOnly = true)
    public List<Task> getOverdueTasks(String userIdHeader) {
        User requester = requesterResolver.resolve(userIdHeader);
        return visibleTasks(requester, taskRepository.findByDueDateBeforeAndStatusNot(
                LocalDate.now(), TaskStatus.DONE));
    }

    // Update a task
    public Task updateTask(Long id, TaskRequest request, String userIdHeader) {
        User requester = requesterResolver.resolve(userIdHeader);

        if (request == null) {
            throw new BadRequestException("Task request is required");
        }

        validateId(id, "taskId");
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        Project project = requireProjectManager(task.getProject().getId(), requester);

        if (request.getProjectId() != null) {
            validateId(request.getProjectId(), "projectId");
            if (!request.getProjectId().equals(project.getId())) {
                throw new BadRequestException("Task projectId cannot be changed");
            }
        }
        if (request.getAssigneeId() != null) {
            validateId(request.getAssigneeId(), "assigneeId");
        }
        if (request.getTitle() != null) {
            if (request.getTitle().isBlank()) {
                throw new BadRequestException("Task title cannot be blank");
            }
            validateTitleLength(request.getTitle());
        }
        validateDescription(request.getDescription());

        if (request.getStatus() != null
                && request.getStatus() != task.getStatus()
                && !isAssignedTo(task, requester)) {
            throw new ForbiddenException("Only the assigned user can change task status");
        }

        LocalDate effectiveStartDate = request.getStartDate() != null
                ? request.getStartDate() : task.getStartDate();
        LocalDate effectiveDueDate = request.getDueDate() != null
                ? request.getDueDate() : task.getDueDate();
        if (effectiveStartDate != null && effectiveDueDate != null
                && effectiveDueDate.isBefore(effectiveStartDate)) {
            throw new BadRequestException(
                    "Due date cannot be before start date");
        }

        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }

        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }

        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }

        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
        }

        if (request.getStartDate() != null) {
            task.setStartDate(request.getStartDate());
        }

        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate());
        }

        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(
                            request.getAssigneeId())
                    .orElseThrow(() ->
                            new NotFoundException("Assignee not found"));

            requireProjectMember(project.getId(), assignee.getId());
            task.setAssignee(assignee);
        }

        return taskRepository.save(task);
    }

    // Update task status
    public Task updateTaskStatus(Long id, TaskStatus status, String userIdHeader) {
        User requester = requesterResolver.resolve(userIdHeader);

        if (status == null) {
            throw new BadRequestException("Status is required");
        }

        validateId(id, "taskId");
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        requireProjectMembership(task.getProject().getId(), requester);
        if (!isAssignedTo(task, requester)) {
            throw new ForbiddenException("Only the assigned user can change task status");
        }
        task.setStatus(status);

        return taskRepository.save(task);
    }

    // Delete a task
    public void deleteTask(Long id, String userIdHeader) {
        User requester = requesterResolver.resolve(userIdHeader);
        validateId(id, "taskId");
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        requireProjectManager(task.getProject().getId(), requester);
        taskRepository.delete(task);
    }

    private List<Task> visibleTasks(User requester, List<Task> tasks) {
        Map<Long, ProjectMember> memberships = projectMemberRepository
                .findAllByUserId(requester.getId()).stream()
                .collect(Collectors.toMap(
                        membership -> membership.getProject().getId(),
                        Function.identity()));

        return tasks.stream()
                .filter(task -> {
                    if (task.getProject() == null) {
                        return false;
                    }
                    ProjectMember membership = memberships.get(task.getProject().getId());
                    return membership != null
                            && ("MANAGER".equals(membership.getMembershipRole())
                            || isAssignedTo(task, requester));
                })
                .toList();
    }

    private void requireTaskVisibility(Task task, User requester) {
        if (task.getProject() == null) {
            throw new ForbiddenException("Project membership is required");
        }
        ProjectMember membership = projectMemberRepository
                .findByProjectIdAndUserId(task.getProject().getId(), requester.getId())
                .orElseThrow(() -> new ForbiddenException("Project membership is required"));
        if (!"MANAGER".equals(membership.getMembershipRole()) && !isAssignedTo(task, requester)) {
            throw new ForbiddenException("Task is not assigned to the requester");
        }
    }

    private Project requireProjectMembership(Long projectId, User requester) {
        validateId(projectId, "projectId");
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));
        projectMemberRepository.findByProjectIdAndUserId(projectId, requester.getId())
                .orElseThrow(() -> new ForbiddenException("Project membership is required"));
        return project;
    }

    private Project requireProjectManager(Long projectId, User requester) {
        validateId(projectId, "projectId");
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));
        ProjectMember membership = projectMemberRepository
                .findByProjectIdAndUserId(projectId, requester.getId())
                .orElseThrow(() -> new ForbiddenException("Project MANAGER membership is required"));
        if (!"MANAGER".equals(membership.getMembershipRole())) {
            throw new ForbiddenException("Project MANAGER membership is required");
        }
        return project;
    }

    private void requireProjectMember(Long projectId, Long userId) {
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new BadRequestException("Assignee must be a member of the task project");
        }
    }

    private boolean isAssignedTo(Task task, User requester) {
        return task.getAssignee() != null
                && requester.getId().equals(task.getAssignee().getId());
    }

    private void validateId(Long id, String field) {
        if (id == null || id <= 0) {
            throw new BadRequestException(field + " must be a positive number");
        }
    }

    private void validateTitleLength(String title) {
        if (title.length() > MAX_TASK_TITLE_LENGTH) {
            throw new BadRequestException("Task title must not exceed 150 characters");
        }
    }

    private void validateDescription(String description) {
        if (description == null) {
            return;
        }
        if (description.length() > MAX_TASK_DESCRIPTION_LENGTH
                || description.getBytes(StandardCharsets.UTF_8).length > MAX_TASK_DESCRIPTION_BYTES) {
            throw new BadRequestException("Task description exceeds the database TEXT column limit");
        }
    }
}
