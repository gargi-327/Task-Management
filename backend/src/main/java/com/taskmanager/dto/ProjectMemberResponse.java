package com.taskmanager.dto;

import com.taskmanager.entity.ProjectMember;

public record ProjectMemberResponse(Long userId, String name, String email, String membershipRole) {
    public static ProjectMemberResponse from(ProjectMember member) {
        return new ProjectMemberResponse(
                member.getUser().getId(),
                member.getUser().getName(),
                member.getUser().getEmail(),
                member.getMembershipRole()
        );
    }
}
