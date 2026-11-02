
package com.taskmanager.dto;

import com.taskmanager.entity.TaskPriority;
import com.taskmanager.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;

import java.time.LocalDate;

public class TaskRequest {

    public interface OnCreate extends Default {
    }

    @NotNull(groups = OnCreate.class, message = "must be provided")
    @Positive(message = "must be a positive number")
    private Long projectId;

    @NotBlank(groups = OnCreate.class, message = "must not be blank")
    @Size(max = 150, message = "must not exceed 150 characters")
    private String title;

    @Size(max = 65_535, message = "must not exceed 65535 characters")
    private String description;

    private TaskPriority priority;
    private TaskStatus status;

    @NotNull(groups = OnCreate.class, message = "must be provided")
    private LocalDate startDate;

    private LocalDate dueDate;

    @Positive(message = "must be a positive number")
    private Long assigneeId;

    private Long createdBy;

    public TaskRequest() {
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
