package com.example.taskmanagement.service;

import com.example.taskmanagement.entity.Task;
import com.example.taskmanagement.entity.Project;
import com.example.taskmanagement.entity.User;
import com.example.taskmanagement.enums.Priority;
import com.example.taskmanagement.enums.TaskStatus;
import com.example.taskmanagement.repository.TaskRepository;
import com.example.taskmanagement.repository.ProjectRepository;
import com.example.taskmanagement.repository.UserRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public TaskService(
            TaskRepository taskRepository,
            ProjectRepository projectRepository,
            UserRepository userRepository) {

        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
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
                        || projectId.equals(task.getProject().getId()))
                .filter(task -> status == null
                        || status == task.getStatus())
                .filter(task -> priority == null
                        || priority == task.getPriority())
                .filter(task -> assigneeId == null
                        || (task.getAssignee() != null
                        && assigneeId.equals(task.getAssignee().getId())))
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
                        new RuntimeException("Task not found with ID: " + id));
    }

    public List<Task> getTasksByProject(Long projectId) {
        return taskRepository.findByProject_Id(projectId);
    }

    public List<Task> getTasksByStatus(TaskStatus status) {
        return taskRepository.findByStatus(status);
    }

    public List<Task> getTasksByPriority(Priority priority) {
        return taskRepository.findByPriority(priority);
    }

    public List<Task> getTasksByAssignee(Long assigneeId) {
        return taskRepository.findByAssignee_Id(assigneeId);
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

        if (updatedTask.getProject() != null) {
            existingTask.setProject(updatedTask.getProject());
        }

        if (updatedTask.getAssignee() != null) {
            existingTask.setAssignee(updatedTask.getAssignee());
        }

        if (updatedTask.getCreator() != null) {
            existingTask.setCreator(updatedTask.getCreator());
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

        if (task.getProject() == null
                || task.getProject().getId() == null) {
            throw new IllegalArgumentException("Project ID is required");
        }

        Project project = projectRepository.findById(
                        task.getProject().getId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Project not found with ID: "
                                        + task.getProject().getId()));

        task.setProject(project);

        if (task.getAssignee() != null
                && task.getAssignee().getId() != null) {

            User assignee = userRepository.findById(
                            task.getAssignee().getId())
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Assignee not found"));

            task.setAssignee(assignee);
        }

        if (task.getCreator() != null
                && task.getCreator().getId() != null) {

            User creator = userRepository.findById(
                            task.getCreator().getId())
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Creator not found"));

            task.setCreator(creator);
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