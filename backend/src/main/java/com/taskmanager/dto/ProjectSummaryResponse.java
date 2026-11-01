package com.taskmanager.dto;

import java.time.LocalDate;

public record ProjectSummaryResponse(
        Long projectId,
        long totalTasks,
        long todoTasks,
        long inProgressTasks,
        long completedTasks,
        long overdueTasks,
        LocalDate calculatedOn
) { }