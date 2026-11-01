
package com.taskmanager.dto;

import com.taskmanager.entity.Task;
import com.taskmanager.entity.TaskPriority;
import com.taskmanager.entity.TaskStatus;

import java.time.LocalDate;

public class TaskResponse {

    private Long id;
    private Long projectId;
    private String title;
    private String description;
    private TaskPriority priority;
    private TaskStatus status;
    private LocalDate startDate;
    private LocalDate dueDate;
    private Long assigneeId;
    private Long createdBy;

    public TaskResponse() {
    }

    public static TaskResponse from(Task task) {
        if (task == null) {
            return null;
        }

        TaskResponse response = new TaskResponse();

        response.setId(task.getId());
        response.setProjectId(
                task.getProject() != null ? task.getProject().getId() : null
        );
        response.setTitle(task.getTitle());
        response.setDescription(task.getDescription());
        response.setPriority(task.getPriority());
        response.setStatus(task.getStatus());
        response.setStartDate(task.getStartDate());
        response.setDueDate(task.getDueDate());
        response.setAssigneeId(
                task.getAssignee() != null ? task.getAssignee().getId() : null
        );
        response.setCreatedBy(
                task.getCreatedBy() != null ? task.getCreatedBy().getId() : null
        );

        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
}
