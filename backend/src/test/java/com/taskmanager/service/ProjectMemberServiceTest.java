package com.taskmanager.service;

import com.taskmanager.dto.ProjectMemberRequest;
import com.taskmanager.dto.ProjectMemberResponse;
import com.taskmanager.entity.Project;
import com.taskmanager.entity.ProjectMember;
import com.taskmanager.entity.User;
import com.taskmanager.exception.BadRequestException;
import com.taskmanager.exception.ConflictException;
import com.taskmanager.exception.ForbiddenException;
import com.taskmanager.exception.NotFoundException;
import com.taskmanager.repository.ProjectMemberRepository;
import com.taskmanager.repository.ProjectRepository;
import com.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectMemberServiceTest {
    @Mock private ProjectMemberRepository memberships;
    @Mock private ProjectRepository projects;
    @Mock private UserRepository users;
    @Mock private RequesterResolver requesterResolver;
    @InjectMocks private ProjectMemberService service;

    @Test
    void createsMembership() {
        Project project = mock(Project.class);
        User user = createUser(7L, "Avery", "avery@example.com");
        stubManagerAuthorization(3L, project);
        when(projects.findById(3L)).thenReturn(Optional.of(project));
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(memberships.existsByProjectIdAndUserId(3L, 7L)).thenReturn(false);
        when(memberships.saveAndFlush(any(ProjectMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectMemberResponse result = service.addMember(3L, "1", new ProjectMemberRequest(7L, "MEMBER"));

        assertEquals(new ProjectMemberResponse(7L, "Avery", "avery@example.com", "MEMBER"), result);
        ArgumentCaptor<ProjectMember> captor = ArgumentCaptor.forClass(ProjectMember.class);
        verify(memberships).saveAndFlush(captor.capture());
        assertSame(project, captor.getValue().getProject());
        assertSame(user, captor.getValue().getUser());
        assertEquals("MEMBER", captor.getValue().getMembershipRole());
    }

    @Test
    void rejectsNonexistentProject() {
        when(requesterResolver.resolve("1")).thenReturn(mock(User.class));
        when(projects.findById(404L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.addMember(404L, "1", new ProjectMemberRequest(7L, "MEMBER")));

        verifyNoInteractions(users, memberships);
    }

    @Test
    void rejectsNonexistentUser() {
        Project project = mock(Project.class);
        stubManagerAuthorization(3L, project);
        when(projects.findById(3L)).thenReturn(Optional.of(project));
        when(users.findById(404L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.addMember(3L, "1", new ProjectMemberRequest(404L, "MEMBER")));

        verify(memberships, never()).saveAndFlush(any());
    }

    @Test
    void rejectsDuplicateMembership() {
        Project project = mock(Project.class);
        stubManagerAuthorization(3L, project);
        when(projects.findById(3L)).thenReturn(Optional.of(project));
        when(users.findById(7L)).thenReturn(Optional.of(mock(User.class)));
        when(memberships.existsByProjectIdAndUserId(3L, 7L)).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> service.addMember(3L, "1", new ProjectMemberRequest(7L, "MEMBER")));

        verify(memberships, never()).saveAndFlush(any());
    }

    @Test
    void rejectsUnsupportedMembershipRole() {
        stubManagerAuthorization(3L, mock(Project.class));
        assertThrows(BadRequestException.class,
                () -> service.addMember(3L, "1", new ProjectMemberRequest(7L, "ADMIN")));

        verify(users, never()).findById(any());
        verify(memberships, never()).saveAndFlush(any());
    }

    @Test
    void retrievesProjectMembers() {
        User user = createUser(7L, "Avery", "avery@example.com");
        ProjectMember membership = new ProjectMember(mock(Project.class), user, "MEMBER");
        Project project = mock(Project.class);
        stubManagerAuthorization(3L, project);
        when(projects.findById(3L)).thenReturn(Optional.of(project));
        when(memberships.findAllByProjectId(3L)).thenReturn(List.of(membership));

        List<ProjectMemberResponse> result = service.findMembers(3L, "1");

        assertEquals(List.of(new ProjectMemberResponse(7L, "Avery", "avery@example.com", "MEMBER")), result);
    }

    @Test
    void removesExistingMembership() {
        ProjectMember membership = mock(ProjectMember.class);
        Project project = mock(Project.class);
        stubManagerRemovalAuthorization(3L, project);
        when(users.findById(7L)).thenReturn(Optional.of(mock(User.class)));
        when(memberships.findByProjectIdAndUserId(3L, 7L)).thenReturn(Optional.of(membership));

        service.removeMember(3L, 7L, "1");

        verify(memberships).delete(membership);
    }

    @Test
    void removesMemberWhenTargetIsAnOrdinaryMember() {
        ProjectMember membership = membershipWithRole("MEMBER");
        stubManagerRemovalAuthorization(3L, mock(Project.class));
        when(users.findById(7L)).thenReturn(Optional.of(mock(User.class)));
        when(memberships.findByProjectIdAndUserId(3L, 7L)).thenReturn(Optional.of(membership));

        service.removeMember(3L, 7L, "1");

        verify(memberships).delete(membership);
        verify(memberships, never()).findAllByProjectIdAndMembershipRoleForUpdate(anyLong(), anyString());
    }

    @Test
    void removesManagerWhenAnotherManagerRemains() {
        ProjectMember membership = membershipWithRole("MANAGER");
        stubManagerRemovalAuthorization(3L, mock(Project.class));
        when(users.findById(7L)).thenReturn(Optional.of(mock(User.class)));
        when(memberships.findByProjectIdAndUserId(3L, 7L)).thenReturn(Optional.of(membership));
        when(memberships.findAllByProjectIdAndMembershipRoleForUpdate(3L, "MANAGER"))
                .thenReturn(List.of(membership, mock(ProjectMember.class)));

        service.removeMember(3L, 7L, "1");

        verify(memberships).delete(membership);
    }

    @Test
    void rejectsRemovingTheOnlyManagerAndLeavesMembershipUnchanged() {
        ProjectMember onlyManager = membershipWithRole("MANAGER");
        stubManagerRemovalAuthorization(3L, mock(Project.class));
        when(users.findById(1L)).thenReturn(Optional.of(mock(User.class)));
        when(memberships.findByProjectIdAndUserId(3L, 1L)).thenReturn(Optional.of(onlyManager));
        when(memberships.findAllByProjectIdAndMembershipRoleForUpdate(3L, "MANAGER"))
                .thenReturn(List.of(onlyManager));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> service.removeMember(3L, 1L, "1"));

        assertEquals("Cannot remove the last MANAGER from a project", exception.getMessage());
        verify(memberships, never()).delete(onlyManager);
        assertSame(onlyManager, memberships.findByProjectIdAndUserId(3L, 1L).orElseThrow());
    }

    @Test
    void rejectsManagerRemovingThemselvesWhenTheyAreTheOnlyManager() {
        ProjectMember onlyManager = membershipWithRole("MANAGER");
        stubManagerRemovalAuthorization(3L, mock(Project.class));
        when(users.findById(1L)).thenReturn(Optional.of(mock(User.class)));
        when(memberships.findByProjectIdAndUserId(3L, 1L)).thenReturn(Optional.of(onlyManager));
        when(memberships.findAllByProjectIdAndMembershipRoleForUpdate(3L, "MANAGER"))
                .thenReturn(List.of(onlyManager));

        assertThrows(BadRequestException.class, () -> service.removeMember(3L, 1L, "1"));

        verify(memberships, never()).delete(onlyManager);
    }

    @Test
    void removesManagerWhenAtLeastTwoOtherManagersRemain() {
        ProjectMember membership = membershipWithRole("MANAGER");
        stubManagerRemovalAuthorization(3L, mock(Project.class));
        when(users.findById(7L)).thenReturn(Optional.of(mock(User.class)));
        when(memberships.findByProjectIdAndUserId(3L, 7L)).thenReturn(Optional.of(membership));
        when(memberships.findAllByProjectIdAndMembershipRoleForUpdate(3L, "MANAGER"))
                .thenReturn(List.of(membership, mock(ProjectMember.class), mock(ProjectMember.class)));

        service.removeMember(3L, 7L, "1");

        verify(memberships).delete(membership);
    }

    @Test
    void rejectsUnauthorizedUserFromRemovingProjectMembers() {
        Project project = mock(Project.class);
        User requester = createRequester(1L);
        when(requesterResolver.resolve("1")).thenReturn(requester);
        when(projects.findByIdForUpdate(3L)).thenReturn(Optional.of(project));
        when(memberships.findByProjectIdAndUserId(3L, 1L))
                .thenReturn(Optional.of(new ProjectMember(project, requester, "MEMBER")));

        assertThrows(ForbiddenException.class, () -> service.removeMember(3L, 7L, "1"));

        verify(memberships, never()).delete(any(ProjectMember.class));
        verify(users, never()).findById(any());
    }

    @Test
    void returnsNotFoundWhenMembershipDoesNotExist() {
        Project project = mock(Project.class);
        stubManagerRemovalAuthorization(3L, project);
        when(users.findById(7L)).thenReturn(Optional.of(mock(User.class)));
        when(memberships.findByProjectIdAndUserId(3L, 7L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.removeMember(3L, 7L, "1"));
    }

    @Test
    void rejectsInvalidProjectId() {
        when(requesterResolver.resolve("1")).thenReturn(mock(User.class));
        assertThrows(BadRequestException.class,
                () -> service.findMembers(0L, "1"));

        verifyNoInteractions(projects, memberships);
    }

    @Test
    void rejectsProjectMemberFromMembershipOperations() {
        Project project = mock(Project.class);
        User requester = createRequester(1L);
        when(requesterResolver.resolve("1")).thenReturn(requester);
        when(projects.findById(3L)).thenReturn(Optional.of(project));
        when(memberships.findByProjectIdAndUserId(3L, 1L))
                .thenReturn(Optional.of(new ProjectMember(project, requester, "MEMBER")));

        assertThrows(ForbiddenException.class, () -> service.findMembers(3L, "1"));
        verify(memberships, never()).findAllByProjectId(any());
    }

    @Test
    void rejectsNonMemberFromMembershipOperations() {
        Project project = mock(Project.class);
        User requester = createRequester(1L);
        when(requesterResolver.resolve("1")).thenReturn(requester);
        when(projects.findById(3L)).thenReturn(Optional.of(project));
        when(memberships.findByProjectIdAndUserId(3L, 1L)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> service.findMembers(3L, "1"));
        verify(memberships, never()).findAllByProjectId(any());
    }

    private void stubManagerAuthorization(Long projectId, Project project) {
        User requester = createRequester(1L);
        when(requesterResolver.resolve("1")).thenReturn(requester);
        when(projects.findById(projectId)).thenReturn(Optional.of(project));
        when(memberships.findByProjectIdAndUserId(projectId, 1L))
                .thenReturn(Optional.of(new ProjectMember(project, requester, "MANAGER")));
    }

    private void stubManagerRemovalAuthorization(Long projectId, Project project) {
        User requester = createRequester(1L);
        when(requesterResolver.resolve("1")).thenReturn(requester);
        when(projects.findByIdForUpdate(projectId)).thenReturn(Optional.of(project));
        when(memberships.findByProjectIdAndUserId(projectId, 1L))
                .thenReturn(Optional.of(new ProjectMember(project, requester, "MANAGER")));
    }

    private ProjectMember membershipWithRole(String role) {
        ProjectMember membership = mock(ProjectMember.class);
        when(membership.getMembershipRole()).thenReturn(role);
        return membership;
    }

    private User createRequester(Long id) {
        User requester = mock(User.class);
        when(requester.getId()).thenReturn(id);
        return requester;
    }

    private User createUser(Long id, String name, String email) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getName()).thenReturn(name);
        when(user.getEmail()).thenReturn(email);
        return user;
    }
}
