package com.taskmanager.dto;

import java.time.LocalDate;

/** Pending is the agreed TODO status; completed is DONE. */
public record ProjectSummaryResponse(
        Long projectId,
        long totalTasks,
        long pendingTasks,
        long inProgressTasks,
        long completedTasks,
        long overdueTasks,
        LocalDate calculatedOn
) { }
