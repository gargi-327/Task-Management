package com.taskmanager.service;

import com.taskmanager.dto.ProjectRequest;
import com.taskmanager.dto.ProjectResponse;
import com.taskmanager.dto.ProjectSummaryResponse;
import com.taskmanager.dto.ProjectUpdateRequest;
import com.taskmanager.entity.Project;
import com.taskmanager.entity.ProjectMember;
import com.taskmanager.entity.Role;
import com.taskmanager.entity.TaskStatus;
import com.taskmanager.entity.User;
import com.taskmanager.exception.BadRequestException;
import com.taskmanager.exception.ForbiddenException;
import com.taskmanager.exception.NotFoundException;
import com.taskmanager.exception.UnauthorizedException;
import com.taskmanager.repository.ProjectMemberRepository;
import com.taskmanager.repository.ProjectRepository;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {
    @Mock private ProjectRepository projects;
    @Mock private UserRepository users;
    @Mock private TaskRepository tasks;
    @Mock private ProjectMemberRepository memberships;
    @Mock private RequesterResolver requesterResolver;
    @InjectMocks private ProjectService service;

    @Test
    void rejectsProjectDueDateBeforeStartDate() {
        User manager = requester(7L, Role.MANAGER);
        when(requesterResolver.resolve("7")).thenReturn(manager);
        ProjectRequest request = new ProjectRequest("Website", null,
                LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 1), 7L);

        assertThrows(BadRequestException.class, () -> service.create(request, "7"));

        verifyNoInteractions(users, projects, memberships);
    }

    @Test
    void rejectsCreatedByIdThatDoesNotMatchRequester() {
        User manager = requester(7L, Role.MANAGER);
        when(requesterResolver.resolve("7")).thenReturn(manager);
        ProjectRequest request = new ProjectRequest("Website", null, null, null, 99L);

        assertThrows(BadRequestException.class, () -> service.create(request, "7"));

        verifyNoInteractions(users, projects, memberships);
    }

    @Test
    void rejectsProjectCreationByGlobalMember() {
        User member = requester(7L, Role.MEMBER);
        when(requesterResolver.resolve("7")).thenReturn(member);
        ProjectRequest request = new ProjectRequest("Website", null, null, null, 7L);

        assertThrows(ForbiddenException.class, () -> service.create(request, "7"));

        verifyNoInteractions(users, projects, memberships);
    }

    @Test
    void createsProjectAndAddsRequesterAsProjectManager() {
        User manager = requester(7L, Role.MANAGER);
        when(requesterResolver.resolve("7")).thenReturn(manager);
        when(projects.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberships.save(any(ProjectMember.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ProjectRequest request = new ProjectRequest(" Website ", "Description", null, null, 7L);

        ProjectResponse response = service.create(request, "7");

        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
        verify(projects).save(projectCaptor.capture());
        assertSame(manager, projectCaptor.getValue().getCreatedBy());
        assertEquals("Website", projectCaptor.getValue().getName());

        ArgumentCaptor<ProjectMember> membershipCaptor = ArgumentCaptor.forClass(ProjectMember.class);
        verify(memberships).save(membershipCaptor.capture());
        assertSame(projectCaptor.getValue(), membershipCaptor.getValue().getProject());
        assertSame(manager, membershipCaptor.getValue().getUser());
        assertEquals("MANAGER", membershipCaptor.getValue().getMembershipRole());
        assertEquals(7L, response.createdById());
        verifyNoInteractions(users);
    }

    @Test
    void missingRequesterIsUnauthorized() {
        when(requesterResolver.resolve(null)).thenThrow(
                new UnauthorizedException("A valid X-User-Id header is required"));

        assertThrows(UnauthorizedException.class, () -> service.findAll(null, false));

        verifyNoInteractions(projects, memberships);
    }

    @Test
    void projectMemberCanViewProjectDetails() {
        User member = requester(7L, Role.MEMBER);
        Project project = project(3L, false, member);
        when(requesterResolver.resolve("7")).thenReturn(member);
        when(projects.findById(3L)).thenReturn(Optional.of(project));
        when(memberships.existsByProjectIdAndUserId(3L, 7L)).thenReturn(true);

        assertEquals(3L, service.findOne(3L, "7").id());
    }

    @Test
    void nonmemberCannotViewProjectDetailsOrSummary() {
        User requester = requester(8L, Role.MEMBER);
        Project project = project(3L, false, requester);
        when(requesterResolver.resolve("8")).thenReturn(requester);
        when(projects.findById(3L)).thenReturn(Optional.of(project));
        when(memberships.existsByProjectIdAndUserId(3L, 8L)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> service.findOne(3L, "8"));
        assertThrows(ForbiddenException.class, () -> service.summary(3L, "8"));
    }

    @Test
    void projectMemberCanViewSummary() {
        User member = requester(7L, Role.MEMBER);
        Project project = project(3L, false, member);
        when(requesterResolver.resolve("7")).thenReturn(member);
        when(projects.findById(3L)).thenReturn(Optional.of(project));
        when(memberships.existsByProjectIdAndUserId(3L, 7L)).thenReturn(true);
        when(tasks.countByProjectId(3L)).thenReturn(4L);
        when(tasks.countByProjectIdAndStatus(3L, TaskStatus.TODO)).thenReturn(1L);
        when(tasks.countByProjectIdAndStatus(3L, TaskStatus.IN_PROGRESS)).thenReturn(2L);
        when(tasks.countByProjectIdAndStatus(3L, TaskStatus.DONE)).thenReturn(1L);

        ProjectSummaryResponse summary = service.summary(3L, "7");

        assertEquals(4L, summary.totalTasks());
        assertEquals(1L, summary.todoTasks());
        assertEquals(2L, summary.inProgressTasks());
        assertEquals(1L, summary.completedTasks());
    }

    @Test
    void listsOnlyMembershipProjectsAndPreservesArchivedOption() {
        User member = requester(7L, Role.MEMBER);
        Project activeProject = project(3L, false, member);
        Project archivedProject = project(4L, true, member);
        Project otherProject = project(5L, false, requester(8L, Role.MANAGER));
        when(requesterResolver.resolve("7")).thenReturn(member);
        when(memberships.findAllByUserId(7L)).thenReturn(List.of(
                new ProjectMember(activeProject, member, "MEMBER"),
                new ProjectMember(archivedProject, member, "MEMBER")));
        when(projects.findByArchivedFalseOrderByIdDesc()).thenReturn(List.of(activeProject, otherProject));
        when(projects.findAll()).thenReturn(List.of(archivedProject, activeProject, otherProject));

        List<ProjectResponse> active = service.findAll("7", false);
        List<ProjectResponse> includingArchived = service.findAll("7", true);

        assertEquals(List.of(3L), active.stream().map(ProjectResponse::id).toList());
        assertEquals(List.of(4L, 3L), includingArchived.stream().map(ProjectResponse::id).toList());
    }

    @Test
    void managerCanUpdateAndArchiveProject() {
        User manager = requester(7L, Role.MEMBER);
        Project project = new Project();
        project.setCreatedBy(manager);
        project.setName("Original");
        when(requesterResolver.resolve("7")).thenReturn(manager);
        when(projects.findById(3L)).thenReturn(Optional.of(project));
        when(memberships.findByProjectIdAndUserId(3L, 7L))
                .thenReturn(Optional.of(new ProjectMember(project, manager, "MANAGER")));
        when(users.findById(7L)).thenReturn(Optional.of(manager));
        ProjectRequest fullUpdate = new ProjectRequest("Full update", null, null, null, 7L);

        assertEquals("Full update", service.update(3L, fullUpdate, "7").name());
        assertEquals("Partial update", service.update(
                3L, new ProjectUpdateRequest("Partial update", null, null, null), "7").name());
        assertTrue(service.archive(3L, "7").archived());
    }

    @Test
    void projectMemberCannotUpdateOrArchiveProject() {
        User member = requester(7L, Role.MEMBER);
        Project project = project(3L, false, member);
        when(requesterResolver.resolve("7")).thenReturn(member);
        when(projects.findById(3L)).thenReturn(Optional.of(project));
        when(memberships.findByProjectIdAndUserId(3L, 7L))
                .thenReturn(Optional.of(new ProjectMember(project, member, "MEMBER")));

        assertThrows(ForbiddenException.class, () -> service.update(
                3L, new ProjectRequest("Update", null, null, null, 7L), "7"));
        assertThrows(ForbiddenException.class, () -> service.update(
                3L, new ProjectUpdateRequest("Update", null, null, null), "7"));
        assertThrows(ForbiddenException.class, () -> service.archive(3L, "7"));
        verifyNoInteractions(users);
    }

    @Test
    void nonmemberCannotUpdateOrArchiveProject() {
        User requester = requester(8L, Role.MANAGER);
        Project project = project(3L, false, requester);
        when(requesterResolver.resolve("8")).thenReturn(requester);
        when(projects.findById(3L)).thenReturn(Optional.of(project));
        when(memberships.findByProjectIdAndUserId(3L, 8L)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> service.update(
                3L, new ProjectRequest("Update", null, null, null, 8L), "8"));
        assertThrows(ForbiddenException.class, () -> service.archive(3L, "8"));
    }

    @Test
    void nonexistentProjectStillReturnsNotFound() {
        User requester = requester(7L, Role.MANAGER);
        when(requesterResolver.resolve("7")).thenReturn(requester);
        when(projects.findById(404L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.archive(404L, "7"));
        verifyNoInteractions(memberships);
    }

    @Test
    void rejectsInvalidProjectId() {
        User manager = requester(7L, Role.MANAGER);
        when(requesterResolver.resolve("7")).thenReturn(manager);

        assertThrows(BadRequestException.class, () -> service.findOne(0L, "7"));

        verifyNoInteractions(projects, memberships);
    }

    private User requester(Long id, Role role) {
        User user = mock(User.class);
        lenient().when(user.getId()).thenReturn(id);
        lenient().when(user.getRole()).thenReturn(role);
        return user;
    }

    private Project project(Long id, boolean archived, User creator) {
        Project project = mock(Project.class);
        lenient().when(project.getId()).thenReturn(id);
        lenient().when(project.getName()).thenReturn("Project " + id);
        lenient().when(project.isArchived()).thenReturn(archived);
        lenient().when(project.getCreatedBy()).thenReturn(creator);
        return project;
    }
}
