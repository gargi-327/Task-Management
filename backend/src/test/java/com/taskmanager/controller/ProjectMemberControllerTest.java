package com.taskmanager.controller;

import com.taskmanager.dto.ProjectMemberResponse;
import com.taskmanager.entity.Project;
import com.taskmanager.entity.ProjectMember;
import com.taskmanager.entity.Role;
import com.taskmanager.entity.User;
import com.taskmanager.repository.ProjectMemberRepository;
import com.taskmanager.repository.ProjectRepository;
import com.taskmanager.repository.UserRepository;
import com.taskmanager.service.ProjectMemberService;
import com.taskmanager.service.RequesterResolver;
import jakarta.servlet.ServletException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProjectMemberController.class)
@Import({ProjectMemberService.class, RequesterResolver.class})
class ProjectMemberControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private ProjectMemberRepository memberships;
    @MockBean private ProjectRepository projects;
    @MockBean private UserRepository users;

    @Test
    void createsMemberForProjectManager() throws Exception {
        stubManagerAuthorization();
        User added = createUser(7L, Role.MEMBER, "Avery", "avery@example.com");
        when(users.findById(7L)).thenReturn(Optional.of(added));
        when(memberships.existsByProjectIdAndUserId(3L, 7L)).thenReturn(false);
        when(memberships.saveAndFlush(any(ProjectMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/projects/3/members")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":7,\"membershipRole\":\"MEMBER\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(7))
                .andExpect(jsonPath("$.membershipRole").value("MEMBER"));
    }

    @Test
    void requiresRequesterHeader() throws Exception {
        assertUnauthorized(post("/api/projects/3/members"));
        assertUnauthorized(get("/api/projects/3/members"));
        assertUnauthorized(delete("/api/projects/3/members/7"));
    }

    @Test
    void rejectsInvalidRequesterId() throws Exception {
        assertUnauthorized(get("/api/projects/3/members").header("X-User-Id", "invalid"));
        assertUnauthorized(get("/api/projects/3/members").header("X-User-Id", "0"));
        assertUnauthorized(get("/api/projects/3/members").header("X-User-Id", "-1"));
    }

    @Test
    void rejectsNonexistentRequester() throws Exception {
        when(users.findById(404L)).thenReturn(Optional.empty());

        assertUnauthorized(get("/api/projects/3/members").header("X-User-Id", "404"));
    }

    @Test
    void rejectsProjectMemberAddingMembers() throws Exception {
        stubAuthorization(Role.MEMBER, true);

        mockMvc.perform(post("/api/projects/3/members")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":7,\"membershipRole\":\"MEMBER\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").exists());
        verify(memberships, never()).saveAndFlush(any());
    }

    @Test
    void rejectsNonMemberAddingMembers() throws Exception {
        stubAuthorization(Role.MEMBER, false);

        mockMvc.perform(post("/api/projects/3/members")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":7,\"membershipRole\":\"MEMBER\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsDuplicateMembershipForProjectManager() throws Exception {
        stubManagerAuthorization();
        User added = createUser(7L, Role.MEMBER, "Avery", "avery@example.com");
        when(users.findById(7L)).thenReturn(Optional.of(added));
        when(memberships.existsByProjectIdAndUserId(3L, 7L)).thenReturn(true);

        mockMvc.perform(post("/api/projects/3/members")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":7,\"membershipRole\":\"MEMBER\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void returnsConflictForMembershipUniqueConstraintViolation() throws Exception {
        stubManagerAuthorization();
        User added = createUser(7L, Role.MEMBER, "Avery", "avery@example.com");
        when(users.findById(7L)).thenReturn(Optional.of(added));
        when(memberships.existsByProjectIdAndUserId(3L, 7L)).thenReturn(false);
        ConstraintViolationException duplicate = new ConstraintViolationException(
                "duplicate", null, "insert into project_members", "uk_project_member");
        when(memberships.saveAndFlush(any(ProjectMember.class)))
                .thenThrow(new DataIntegrityViolationException("database detail", duplicate));

        mockMvc.perform(post("/api/projects/3/members")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":7,\"membershipRole\":\"MEMBER\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "User 7 is already a member of project 3"));
    }

    @Test
    void returnsBadRequestForForeignKeyIntegrityViolationWithoutDatabaseDetails() throws Exception {
        stubManagerAuthorization();
        User added = createUser(7L, Role.MEMBER, "Avery", "avery@example.com");
        when(users.findById(7L)).thenReturn(Optional.of(added));
        when(memberships.existsByProjectIdAndUserId(3L, 7L)).thenReturn(false);
        ConstraintViolationException foreignKey = new ConstraintViolationException(
                "private database detail", null, "insert into project_members", "fk_project_member_user");
        when(memberships.saveAndFlush(any(ProjectMember.class)))
                .thenThrow(new DataIntegrityViolationException("private database detail", foreignKey));

        mockMvc.perform(post("/api/projects/3/members")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":7,\"membershipRole\":\"MEMBER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Request violates a data integrity constraint"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("private database detail"))));
    }

    @Test
    void doesNotConvertUnexpectedPersistenceProgrammingErrorsIntoClientErrors() {
        stubManagerAuthorization();
        User added = createUser(7L, Role.MEMBER, "Avery", "avery@example.com");
        when(users.findById(7L)).thenReturn(Optional.of(added));
        when(memberships.existsByProjectIdAndUserId(3L, 7L)).thenReturn(false);
        when(memberships.saveAndFlush(any(ProjectMember.class)))
                .thenThrow(new IllegalStateException("programming error"));

        assertThrows(ServletException.class, () -> mockMvc.perform(post("/api/projects/3/members")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":7,\"membershipRole\":\"MEMBER\"}")));
    }

    @Test
    void returnsNotFoundForMissingProject() throws Exception {
        User requester = createUser(1L, Role.MANAGER, "Manager", "manager@example.com");
        when(users.findById(1L)).thenReturn(Optional.of(requester));
        when(projects.findById(404L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/projects/404/members").header("X-User-Id", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Project 404 was not found"));
    }

    @Test
    void returnsNotFoundForMissingAddedUser() throws Exception {
        stubManagerAuthorization();
        when(users.findById(404L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/projects/3/members")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":404,\"membershipRole\":\"MEMBER\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User 404 was not found"));
    }

    @Test
    void listsMembersForProjectManager() throws Exception {
        stubManagerAuthorization();
        User member = createUser(7L, Role.MEMBER, "Avery", "avery@example.com");
        when(memberships.findAllByProjectId(3L))
                .thenReturn(List.of(new ProjectMember(mock(Project.class), member, "MEMBER")));

        mockMvc.perform(get("/api/projects/3/members").header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(7))
                .andExpect(jsonPath("$[0].name").value("Avery"));
    }

    @Test
    void rejectsProjectMemberListingMembers() throws Exception {
        stubAuthorization(Role.MEMBER, true);

        mockMvc.perform(get("/api/projects/3/members").header("X-User-Id", "1"))
                .andExpect(status().isForbidden());
        verify(memberships, never()).findAllByProjectId(3L);
    }

    @Test
    void removesMemberForProjectManager() throws Exception {
        stubManagerAuthorization();
        User removed = createUser(7L, Role.MEMBER, "Avery", "avery@example.com");
        when(users.findById(7L)).thenReturn(Optional.of(removed));
        ProjectMember membership = mock(ProjectMember.class);
        when(memberships.findByProjectIdAndUserId(3L, 7L)).thenReturn(Optional.of(membership));

        mockMvc.perform(delete("/api/projects/3/members/7").header("X-User-Id", "1"))
                .andExpect(status().isNoContent());
        verify(memberships).delete(membership);
    }

    @Test
    void rejectsProjectMemberRemovingMembers() throws Exception {
        stubAuthorization(Role.MEMBER, true);

        mockMvc.perform(delete("/api/projects/3/members/7").header("X-User-Id", "1"))
                .andExpect(status().isForbidden());
        verify(memberships, never()).delete(any(ProjectMember.class));
    }

    @Test
    void returnsNotFoundWhenRequestedRemovalUserIsNotMember() throws Exception {
        stubManagerAuthorization();
        User removed = createUser(7L, Role.MEMBER, "Avery", "avery@example.com");
        when(users.findById(7L)).thenReturn(Optional.of(removed));
        when(memberships.findByProjectIdAndUserId(3L, 7L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/projects/3/members/7").header("X-User-Id", "1"))
                .andExpect(status().isNotFound());
    }

    private void assertUnauthorized(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        mockMvc.perform(request
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":7,\"membershipRole\":\"MEMBER\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("A valid X-User-Id header is required"));
    }

    private void stubManagerAuthorization() {
        stubAuthorization(Role.MANAGER, true);
    }

    private void stubAuthorization(Role role, boolean hasMembership) {
        User requester = createUser(1L, role, "Requester", "requester@example.com");
        Project project = mock(Project.class);
        when(users.findById(1L)).thenReturn(Optional.of(requester));
        when(projects.findById(3L)).thenReturn(Optional.of(project));
        when(projects.findByIdForUpdate(3L)).thenReturn(Optional.of(project));
        if (hasMembership) {
            when(memberships.findByProjectIdAndUserId(3L, 1L))
                    .thenReturn(Optional.of(new ProjectMember(project, requester, role.name())));
        } else {
            when(memberships.findByProjectIdAndUserId(3L, 1L)).thenReturn(Optional.empty());
        }
    }

    private User createUser(Long id, Role role, String name, String email) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getRole()).thenReturn(role);
        when(user.getName()).thenReturn(name);
        when(user.getEmail()).thenReturn(email);
        return user;
    }
}
