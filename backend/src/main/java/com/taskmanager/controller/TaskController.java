
package com.taskmanager.controller;

import com.taskmanager.dto.TaskRequest;
import com.taskmanager.dto.TaskResponse;
import com.taskmanager.entity.Task;
import com.taskmanager.entity.TaskPriority;
import com.taskmanager.entity.TaskStatus;
import com.taskmanager.service.TaskService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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
    public TaskResponse createTask(@RequestBody TaskRequest request) {
        return TaskResponse.from(taskService.createTask(request));
    }

    // Get all tasks with optional filters
    @GetMapping
    public List<TaskResponse> getAllTasks(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dueDate) {

        List<Task> tasks = taskService.getAllTasks();

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
    public TaskResponse getTaskById(@PathVariable Long id) {
        return TaskResponse.from(taskService.getTaskById(id));
    }

    // Get tasks by project
    @GetMapping("/project/{projectId}")
    public List<TaskResponse> getTasksByProject(
            @PathVariable Long projectId) {
        return taskService.getTasksByProject(projectId)
                .stream().map(TaskResponse::from).toList();
    }

    // Get tasks by status
    @GetMapping("/status/{status}")
    public List<TaskResponse> getTasksByStatus(
            @PathVariable TaskStatus status) {
        return taskService.getTasksByStatus(status)
                .stream().map(TaskResponse::from).toList();
    }

    // Get tasks by priority
    @GetMapping("/priority/{priority}")
    public List<TaskResponse> getTasksByPriority(
            @PathVariable TaskPriority priority) {
        return taskService.getTasksByPriority(priority)
                .stream().map(TaskResponse::from).toList();
    }

    // Get tasks by due date
    @GetMapping("/due-date/{dueDate}")
    public List<TaskResponse> getTasksByDueDate(
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dueDate) {
        return taskService.getTasksByDueDate(dueDate)
                .stream().map(TaskResponse::from).toList();
    }

    // Get tasks assigned to a user
    @GetMapping("/assignee/{assigneeId}")
    public List<TaskResponse> getTasksByAssignee(
            @PathVariable Long assigneeId) {
        return taskService.getTasksByAssignee(assigneeId)
                .stream().map(TaskResponse::from).toList();
    }

    // Get overdue tasks
    @GetMapping("/overdue")
    public List<TaskResponse> getOverdueTasks() {
        return taskService.getOverdueTasks()
                .stream().map(TaskResponse::from).toList();
    }

    // Update task
    @PutMapping("/{id}")
    public TaskResponse updateTask(
            @PathVariable Long id,
            @RequestBody TaskRequest request) {
        return TaskResponse.from(taskService.updateTask(id, request));
    }

    // Update task status
    @PatchMapping("/{id}/status")
    public TaskResponse updateTaskStatus(
            @PathVariable Long id,
            @RequestParam TaskStatus status) {
        return TaskResponse.from(
                taskService.updateTaskStatus(id, status));
    }

    // Delete task
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
    }
}
