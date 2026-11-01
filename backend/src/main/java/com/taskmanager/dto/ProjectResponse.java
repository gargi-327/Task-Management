package com.taskmanager.dto;

import com.taskmanager.entity.Project;
import java.time.LocalDate;

public record ProjectResponse(Long id, String name, String description, LocalDate startDate,
                              LocalDate dueDate, boolean archived, Long createdById) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(project.getId(), project.getName(), project.getDescription(),
                project.getStartDate(), project.getDueDate(), project.isArchived(), project.getCreatedBy().getId());
    }
}