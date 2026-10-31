package com.example.taskmanagement.controller;

import com.example.taskmanagement.dto.TaskRequest;
import com.example.taskmanagement.dto.TaskResponse;
import com.example.taskmanagement.entity.Task;
import com.example.taskmanagement.entity.Project;
import com.example.taskmanagement.entity.User;
import com.example.taskmanagement.enums.Priority;
import com.example.taskmanagement.enums.TaskStatus;
import com.example.taskmanagement.service.TaskService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    // Convert request DTO into Task entity
    private Task toEntity(TaskRequest request) {
        Task task = new Task();

        if (request.getProjectId() != null) {
            Project project = new Project();
            project.setId(request.getProjectId());
            task.setProject(project);
        }

        if (request.getAssigneeId() != null) {
            User user = new User();
            user.setId(request.getAssigneeId());
            task.setAssignee(user);
        }

        if (request.getCreatedBy() != null) {
            User creator = new User();
            creator.setId(request.getCreatedBy());
            task.setCreator(creator);
        }

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());
        task.setStatus(request.getStatus());
        task.setStartDate(request.getStartDate());
        task.setDueDate(request.getDueDate());

        return task;
    }

    // Convert Task entity into response DTO
    private TaskResponse toResponse(Task task) {
        TaskResponse response = new TaskResponse();

        response.setId(task.getId());
        response.setProjectId(task.getProject().getId());
        response.setTitle(task.getTitle());
        response.setDescription(task.getDescription());
        response.setPriority(task.getPriority());
        response.setStatus(task.getStatus());
        response.setStartDate(task.getStartDate());
        response.setDueDate(task.getDueDate());

        response.setAssigneeId(
                task.getAssignee() == null
                        ? null : task.getAssignee().getId());

        response.setCreatedBy(
                task.getCreator() == null
                        ? null : task.getCreator().getId());

        return response;
    }

    @PostMapping
    public TaskResponse createTask(@RequestBody TaskRequest request) {
        return toResponse(taskService.createTask(toEntity(request)));
    }

    @GetMapping
    public List<TaskResponse> getTasks(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dueDate) {

        return taskService.getFilteredTasks(
                        projectId, status, priority, assigneeId, dueDate)
                .stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public TaskResponse getTaskById(@PathVariable Long id) {
        return toResponse(taskService.getTaskById(id));
    }

    @GetMapping("/project/{projectId}")
    public List<TaskResponse> getTasksByProject(
            @PathVariable Long projectId) {
        return taskService.getTasksByProject(projectId)
                .stream().map(this::toResponse).toList();
    }

    @GetMapping("/status/{status}")
    public List<TaskResponse> getTasksByStatus(
            @PathVariable TaskStatus status) {
        return taskService.getTasksByStatus(status)
                .stream().map(this::toResponse).toList();
    }

    @GetMapping("/priority/{priority}")
    public List<TaskResponse> getTasksByPriority(
            @PathVariable Priority priority) {
        return taskService.getTasksByPriority(priority)
                .stream().map(this::toResponse).toList();
    }

    @GetMapping("/due-date/{dueDate}")
    public List<TaskResponse> getTasksByDueDate(
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dueDate) {
        return taskService.getTasksByDueDate(dueDate)
                .stream().map(this::toResponse).toList();
    }

    @GetMapping("/assignee/{assigneeId}")
    public List<TaskResponse> getTasksByAssignee(
            @PathVariable Long assigneeId) {
        return taskService.getTasksByAssignee(assigneeId)
                .stream().map(this::toResponse).toList();
    }

    @GetMapping("/overdue")
    public List<TaskResponse> getOverdueTasks() {
        return taskService.getOverdueTasks()
                .stream().map(this::toResponse).toList();
    }

    @PutMapping("/{id}")
    public TaskResponse updateTask(
            @PathVariable Long id,
            @RequestBody TaskRequest request) {
        return toResponse(
                taskService.updateTask(id, toEntity(request)));
    }

    @PatchMapping("/{id}/status")
    public TaskResponse updateTaskStatus(
            @PathVariable Long id,
            @RequestBody StatusRequest request) {
        return toResponse(
                taskService.updateTaskStatus(id, request.getStatus()));
    }

    @DeleteMapping("/{id}")
    public String deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return "Task deleted successfully";
    }

    public static class StatusRequest {
        private TaskStatus status;

        public TaskStatus getStatus() {
            return status;
        }

        public void setStatus(TaskStatus status) {
            this.status = status;
        }
    }
}