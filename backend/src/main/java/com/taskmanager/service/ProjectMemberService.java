package com.taskmanager.service;

import com.taskmanager.dto.ProjectMemberRequest;
import com.taskmanager.dto.ProjectMemberResponse;
import com.taskmanager.entity.Project;
import com.taskmanager.entity.ProjectMember;
import com.taskmanager.entity.User;
import com.taskmanager.exception.BadRequestException;
import com.taskmanager.exception.ConflictException;
import com.taskmanager.exception.NotFoundException;
import com.taskmanager.exception.ForbiddenException;
import com.taskmanager.repository.ProjectMemberRepository;
import com.taskmanager.repository.ProjectRepository;
import com.taskmanager.repository.UserRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProjectMemberService {
    private final ProjectMemberRepository memberships;
    private final ProjectRepository projects;
    private final UserRepository users;
    private final RequesterResolver requesterResolver;

    public ProjectMemberService(
            ProjectMemberRepository memberships,
            ProjectRepository projects,
            UserRepository users,
            RequesterResolver requesterResolver) {
        this.memberships = memberships;
        this.projects = projects;
        this.users = users;
        this.requesterResolver = requesterResolver;
    }

    public ProjectMemberResponse addMember(Long projectId, String userIdHeader, ProjectMemberRequest request) {
        Project project = authorizeProjectManager(projectId, userIdHeader);
        validateId(projectId, "projectId");
        if (request == null) throw new BadRequestException("Request body is required");
        validateId(request.userId(), "userId");
        if (!"MANAGER".equals(request.membershipRole()) && !"MEMBER".equals(request.membershipRole())) {
            throw new BadRequestException("membershipRole must be MANAGER or MEMBER");
        }

        User user = getUser(request.userId());
        if (memberships.existsByProjectIdAndUserId(projectId, request.userId())) {
            throw duplicateMembership(projectId, request.userId());
        }

        try {
            return ProjectMemberResponse.from(memberships.saveAndFlush(
                    new ProjectMember(project, user, request.membershipRole())));
        } catch (DataIntegrityViolationException ex) {
            if (isDuplicateMembershipViolation(ex)) {
                throw duplicateMembership(projectId, request.userId());
            }
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> findMembers(Long projectId, String userIdHeader) {
        authorizeProjectManager(projectId, userIdHeader);
        validateId(projectId, "projectId");
        return memberships.findAllByProjectId(projectId).stream()
                .map(ProjectMemberResponse::from)
                .toList();
    }

    public void removeMember(Long projectId, Long userId, String userIdHeader) {
        User requester = requesterResolver.resolve(userIdHeader);
        validateId(projectId, "projectId");
        Project project = projects.findByIdForUpdate(projectId)
                .orElseThrow(() -> new NotFoundException("Project " + projectId + " was not found"));
        authorizeProjectManager(projectId, requester, project);
        validateId(userId, "userId");
        getUser(userId);
        ProjectMember membership = memberships.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new NotFoundException(
                        "User " + userId + " is not a member of project " + projectId));
        if ("MANAGER".equals(membership.getMembershipRole())
                && memberships.findAllByProjectIdAndMembershipRoleForUpdate(projectId, "MANAGER").size() <= 1) {
            throw new BadRequestException("Cannot remove the last MANAGER from a project");
        }
        memberships.delete(membership);
    }

    private Project getProject(Long projectId) {
        return projects.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project " + projectId + " was not found"));
    }

    private Project authorizeProjectManager(Long projectId, String userIdHeader) {
        User requester = requesterResolver.resolve(userIdHeader);
        validateId(projectId, "projectId");
        Project project = getProject(projectId);
        return authorizeProjectManager(projectId, requester, project);
    }

    private Project authorizeProjectManager(Long projectId, User requester, Project project) {
        ProjectMember requesterMembership = memberships.findByProjectIdAndUserId(projectId, requester.getId())
                .orElseThrow(() -> new ForbiddenException("Project MANAGER membership is required"));
        if (!"MANAGER".equals(requesterMembership.getMembershipRole())) {
            throw new ForbiddenException("Project MANAGER membership is required");
        }
        return project;
    }

    private User getUser(Long userId) {
        return users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId + " was not found"));
    }

    private void validateId(Long id, String field) {
        if (id == null || id <= 0) throw new BadRequestException(field + " must be a positive number");
    }

    private ConflictException duplicateMembership(Long projectId, Long userId) {
        return new ConflictException("User " + userId + " is already a member of project " + projectId);
    }

    private boolean isDuplicateMembershipViolation(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof DuplicateKeyException) {
                return true;
            }
            if (cause instanceof ConstraintViolationException violation) {
                return "uk_project_member".equalsIgnoreCase(violation.getConstraintName());
            }
        }
        return false;
    }
}
