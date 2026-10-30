
package com.example.taskmanagement.service;

import com.example.taskmanagement.entity.Task;
import com.example.taskmanagement.enums.Priority;
import com.example.taskmanagement.enums.TaskStatus;
import com.example.taskmanagement.repository.TaskRepository;
import com.example.taskmanagement.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;

    public TaskService(
            TaskRepository taskRepository,
            ProjectRepository projectRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
    }

    public Task createTask(Task task) {
        validateTask(task);

        if (task.getStatus() == null) {
            task.setStatus(TaskStatus.TODO);
        }

        if (task.getPriority() == null) {
            task.setPriority(Priority.MEDIUM);
        }

        return taskRepository.save(task);
    }

    public List<Task> getFilteredTasks(
            Long projectId,
            TaskStatus status,
            Priority priority,
            Long assigneeId,
            LocalDate dueDate) {

        return taskRepository.findAll().stream()
                .filter(task -> projectId == null
                        || projectId.equals(task.getProjectId()))
                .filter(task -> status == null
                        || status == task.getStatus())
                .filter(task -> priority == null
                        || priority == task.getPriority())
                .filter(task -> assigneeId == null
                        || assigneeId.equals(task.getAssigneeId()))
                .filter(task -> dueDate == null
                        || dueDate.equals(task.getDueDate()))
                .toList();
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public Task getTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Task not found with ID: " + id));
    }

    public List<Task> getTasksByProject(Long projectId) {
        return taskRepository.findByProjectId(projectId);
    }

    public List<Task> getTasksByStatus(TaskStatus status) {
        return taskRepository.findByStatus(status);
    }

    public List<Task> getTasksByPriority(Priority priority) {
        return taskRepository.findByPriority(priority);
    }

    public List<Task> getTasksByAssignee(Long assigneeId) {
        return taskRepository.findByAssigneeId(assigneeId);
    }

    public List<Task> getOverdueTasks() {
        return taskRepository.findByDueDateBeforeAndStatusNot(
                LocalDate.now(), TaskStatus.DONE);
    }

    public List<Task> getTasksByDueDate(LocalDate dueDate) {
        return taskRepository.findByDueDate(dueDate);
    }

    public Task updateTask(Long id, Task updatedTask) {
        Task existingTask = getTaskById(id);

        if (updatedTask.getTitle() != null) {
            existingTask.setTitle(updatedTask.getTitle());
        }

        existingTask.setDescription(updatedTask.getDescription());

        if (updatedTask.getPriority() != null) {
            existingTask.setPriority(updatedTask.getPriority());
        }

        if (updatedTask.getStatus() != null
                && updatedTask.getStatus() != existingTask.getStatus()) {
            throw new IllegalArgumentException(
                    "Use the status update endpoint to change task status");
        }

        if (updatedTask.getStartDate() != null) {
            existingTask.setStartDate(updatedTask.getStartDate());
        }

        if (updatedTask.getDueDate() != null) {
            existingTask.setDueDate(updatedTask.getDueDate());
        }

        if (updatedTask.getAssigneeId() != null) {
            existingTask.setAssigneeId(updatedTask.getAssigneeId());
        }

        validateTask(existingTask);

        return taskRepository.save(existingTask);
    }

    public Task updateTaskStatus(Long id, TaskStatus status) {
        Task task = getTaskById(id);

        if (status == null) {
            throw new IllegalArgumentException("Status is required");
        }

        TaskStatus currentStatus = task.getStatus();

        if (currentStatus == status) {
            return task;
        }

        boolean allowed =
                (currentStatus == TaskStatus.TODO
                        && status == TaskStatus.IN_PROGRESS)
                        || (currentStatus == TaskStatus.IN_PROGRESS
                        && (status == TaskStatus.TODO
                        || status == TaskStatus.DONE));

        if (!allowed) {
            throw new IllegalArgumentException(
                    "Invalid status transition from "
                            + currentStatus + " to " + status);
        }

        task.setStatus(status);
        return taskRepository.save(task);
    }

    public void deleteTask(Long id) {
        Task task = getTaskById(id);
        taskRepository.delete(task);
    }

    private void validateTask(Task task) {
        if (task.getProjectId() == null) {
            throw new IllegalArgumentException("Project ID is required");
        }

        if (!projectRepository.existsById(task.getProjectId())) {
            throw new IllegalArgumentException(
                    "Project not found with ID: " + task.getProjectId());
        }

        if (task.getTitle() == null || task.getTitle().isBlank()) {
            throw new IllegalArgumentException("Task title is required");
        }

        if (task.getStartDate() != null
                && task.getDueDate() != null
                && task.getDueDate().isBefore(task.getStartDate())) {
            throw new IllegalArgumentException(
                    "Due date cannot be before start date");
        }
    }
}
