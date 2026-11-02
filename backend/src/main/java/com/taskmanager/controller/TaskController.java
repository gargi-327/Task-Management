
package com.taskmanager.controller;

import com.taskmanager.dto.TaskRequest;
import com.taskmanager.dto.TaskResponse;
import com.taskmanager.entity.Task;
import com.taskmanager.entity.TaskPriority;
import com.taskmanager.entity.TaskStatus;
import com.taskmanager.service.TaskService;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    // Create task
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @Validated({Default.class, TaskRequest.OnCreate.class}) @RequestBody TaskRequest request) {
        return TaskResponse.from(taskService.createTask(request, userIdHeader));
    }

    // Get all tasks with optional filters
    @GetMapping
    public List<TaskResponse> getAllTasks(
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dueDate) {

        List<Task> tasks = taskService.getAllTasks(userIdHeader, projectId);

        if (projectId != null) {
            tasks = tasks.stream()
                    .filter(t -> t.getProject() != null &&
                            t.getProject().getId().equals(projectId))
                    .toList();
        }

        if (status != null) {
            tasks = tasks.stream()
                    .filter(t -> t.getStatus() == status)
                    .toList();
        }

        if (priority != null) {
            tasks = tasks.stream()
                    .filter(t -> t.getPriority() == priority)
                    .toList();
        }

        if (assigneeId != null) {
            tasks = tasks.stream()
                    .filter(t -> t.getAssignee() != null &&
                            t.getAssignee().getId().equals(assigneeId))
                    .toList();
        }

        if (dueDate != null) {
            tasks = tasks.stream()
                    .filter(t -> dueDate.equals(t.getDueDate()))
                    .toList();
        }

        return tasks.stream()
                .map(TaskResponse::from)
                .toList();
    }

    // Get task by ID
    @GetMapping("/{id}")
    public TaskResponse getTaskById(
            @PathVariable Long id,
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader) {
        return TaskResponse.from(taskService.getTaskById(id, userIdHeader));
    }

    // Get tasks by project
    @GetMapping("/project/{projectId}")
    public List<TaskResponse> getTasksByProject(
            @PathVariable Long projectId,
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader) {
        return taskService.getTasksByProject(projectId, userIdHeader)
                .stream().map(TaskResponse::from).toList();
    }

    // Get tasks by status
    @GetMapping("/status/{status}")
    public List<TaskResponse> getTasksByStatus(
            @PathVariable TaskStatus status,
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader) {
        return taskService.getTasksByStatus(status, userIdHeader)
                .stream().map(TaskResponse::from).toList();
    }

    // Get tasks by priority
    @GetMapping("/priority/{priority}")
    public List<TaskResponse> getTasksByPriority(
            @PathVariable TaskPriority priority,
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader) {
        return taskService.getTasksByPriority(priority, userIdHeader)
                .stream().map(TaskResponse::from).toList();
    }

    // Get tasks by due date
    @GetMapping("/due-date/{dueDate}")
    public List<TaskResponse> getTasksByDueDate(
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dueDate,
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader) {
        return taskService.getTasksByDueDate(dueDate, userIdHeader)
                .stream().map(TaskResponse::from).toList();
    }

    // Get tasks assigned to a user
    @GetMapping("/assignee/{assigneeId}")
    public List<TaskResponse> getTasksByAssignee(
            @PathVariable Long assigneeId,
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader) {
        return taskService.getTasksByAssignee(assigneeId, userIdHeader)
                .stream().map(TaskResponse::from).toList();
    }

    // Get overdue tasks
    @GetMapping("/overdue")
    public List<TaskResponse> getOverdueTasks(
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader) {
        return taskService.getOverdueTasks(userIdHeader)
                .stream().map(TaskResponse::from).toList();
    }

    // Update task
    @PutMapping("/{id}")
    public TaskResponse updateTask(
            @PathVariable Long id,
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @Valid @RequestBody TaskRequest request) {
        return TaskResponse.from(taskService.updateTask(id, request, userIdHeader));
    }

    // Update task status
    @PatchMapping("/{id}/status")
    public TaskResponse updateTaskStatus(
            @PathVariable Long id,
            @RequestParam TaskStatus status,
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader) {
        return TaskResponse.from(
                taskService.updateTaskStatus(id, status, userIdHeader));
    }

    // Delete task
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(
            @PathVariable Long id,
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader) {
        taskService.deleteTask(id, userIdHeader);
    }
}
