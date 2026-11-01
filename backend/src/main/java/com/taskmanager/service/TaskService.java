
package com.taskmanager.service;

import com.taskmanager.dto.TaskRequest;
import com.taskmanager.entity.*;
import com.taskmanager.exception.BadRequestException;
import com.taskmanager.exception.NotFoundException;
import com.taskmanager.repository.ProjectRepository;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.repository.UserRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository,
                       ProjectRepository projectRepository,
                       UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    // Create a task
    public Task createTask(TaskRequest request) {

        if (request == null) {
            throw new BadRequestException("Task request is required");
        }

        if (request.getTitle() == null ||
                request.getTitle().isBlank()) {
            throw new BadRequestException("Task title is required");
        }

        if (request.getProjectId() == null) {
            throw new BadRequestException("Project ID is required");
        }

        if (request.getCreatedBy() == null) {
            throw new BadRequestException("Creator ID is required");
        }

        if (request.getStartDate() == null) {
            throw new BadRequestException("Start date is required");
        }

        if (request.getDueDate() != null &&
                request.getDueDate().isBefore(request.getStartDate())) {
            throw new BadRequestException(
                    "Due date cannot be before start date");
        }

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() ->
                        new NotFoundException("Project not found"));

        User creator = userRepository.findById(request.getCreatedBy())
                .orElseThrow(() ->
                        new NotFoundException("Creator not found"));

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
        task.setCreatedBy(creator);

        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(
                            request.getAssigneeId())
                    .orElseThrow(() ->
                            new NotFoundException("Assignee not found"));

            task.setAssignee(assignee);
        }

        return taskRepository.save(task);
    }

    // Get all tasks
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    // Get task by ID
    public Task getTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("Task not found"));
    }

    // Get tasks by project
    public List<Task> getTasksByProject(Long projectId) {
        return taskRepository.findByProject_Id(projectId);
    }

    // Get tasks by status
    public List<Task> getTasksByStatus(TaskStatus status) {
        return taskRepository.findByStatus(status);
    }

    // Get tasks by priority
    public List<Task> getTasksByPriority(TaskPriority priority) {
        return taskRepository.findByPriority(priority);
    }

    // Get tasks by due date
    public List<Task> getTasksByDueDate(LocalDate dueDate) {
        return taskRepository.findByDueDate(dueDate);
    }

    // Get tasks assigned to a user
    public List<Task> getTasksByAssignee(Long assigneeId) {
        return taskRepository.findByAssignee_Id(assigneeId);
    }

    // Get overdue tasks
    public List<Task> getOverdueTasks() {
        return taskRepository.findByDueDateBeforeAndStatusNot(
                LocalDate.now(), TaskStatus.DONE);
    }

    // Update a task
    public Task updateTask(Long id, TaskRequest request) {

        if (request == null) {
            throw new BadRequestException("Task request is required");
        }

        Task task = getTaskById(id);

        if (request.getTitle() != null) {
            if (request.getTitle().isBlank()) {
                throw new BadRequestException("Task title cannot be blank");
            }
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

        if (request.getDueDate() != null &&
                request.getStartDate() != null &&
                request.getDueDate().isBefore(request.getStartDate())) {
            throw new BadRequestException(
                    "Due date cannot be before start date");
        }

        if (request.getProjectId() != null) {
            Project project = projectRepository.findById(
                            request.getProjectId())
                    .orElseThrow(() ->
                            new NotFoundException("Project not found"));

            task.setProject(project);
        }

        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(
                            request.getAssigneeId())
                    .orElseThrow(() ->
                            new NotFoundException("Assignee not found"));

            task.setAssignee(assignee);
        }

        return taskRepository.save(task);
    }

    // Update task status
    public Task updateTaskStatus(Long id, TaskStatus status) {

        if (status == null) {
            throw new BadRequestException("Status is required");
        }

        Task task = getTaskById(id);
        task.setStatus(status);

        return taskRepository.save(task);
    }

    // Delete a task
    public void deleteTask(Long id) {
        Task task = getTaskById(id);
        taskRepository.delete(task);
    }
}
