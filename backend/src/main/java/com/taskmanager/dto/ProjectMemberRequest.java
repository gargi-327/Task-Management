package com.taskmanager.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record ProjectMemberRequest(
        @NotNull @Positive Long userId,
        @NotNull @Pattern(regexp = "MANAGER|MEMBER", message = "must be MANAGER or MEMBER") String membershipRole
) { }
