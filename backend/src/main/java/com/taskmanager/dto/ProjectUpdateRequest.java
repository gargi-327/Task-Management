package com.taskmanager.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Nullable fields mean "leave the existing value unchanged". The creator and
 * archive flag are deliberately absent so they cannot be modified by PUT.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProjectUpdateRequest(
        @Size(max = 150) String name,
        @Size(max = 2000) String description,
        LocalDate startDate,
        LocalDate dueDate
) { }
