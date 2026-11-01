package com.taskmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ProjectRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 2000) String description,
        LocalDate startDate,
        LocalDate dueDate,
        @NotNull Long createdById
) { }