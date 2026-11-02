package com.taskmanager.service;

import com.taskmanager.dto.TaskRequest;
import com.taskmanager.entity.*;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {
    @Mock private TaskRepository tasks;
    @Mock private ProjectRepository projects;
    @Mock private UserRepository users;
    @Mock private ProjectMemberRepository memberships;
    @Mock private RequesterResolver requesterResolver;
    @InjectMocks private TaskService service;

    @Test
    void missingOrInvalidRequesterIsRejectedBeforeTaskAccess() {
        when(requesterResolver.resolve(null))
                .thenThrow(new UnauthorizedException("Invalid requester"));

        assertThrows(UnauthorizedException.class, () -> service.getAllTasks(null, null));

        verifyNoInteractions(tasks, memberships);
    }

    @Test
    void createsTaskUsingRequesterAndAddsDefaultsIgnoringLegacyCreatedBy() {
        User requester = user(1L);
        Project project = project(3L);
        when(requesterResolver.resolve("1")).thenReturn(requester);
        when(projects.findById(3L)).thenReturn(Optional.of(project));
        when(memberships.findByProjectIdAndUserId(3L, 1L))
                .thenReturn(Optional.of(membership(project, requester, "MANAGER")));
        when(tasks.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        TaskRequest request = request(3L, "Prepare release");
        request.setCreatedBy(99L);

        Task created = service.createTask(request, "1");

        assertSame(requester, created.getCreatedBy());
        assertSame(project, created.getProject());
        assertEquals(TaskPriority.MEDIUM, created.getPriority());
        assertEquals(TaskStatus.TODO, created.getStatus());
        verify(users, never()).findById(any());
    }

    @Test
    void rejectsInvalidCreationValuesBeforeDatabaseAccess() {
        when(requesterResolver.resolve("1")).thenReturn(user(1L));

        TaskRequest invalidProject = request(0L, "Task");
        TaskRequest invalidAssignee = request(3L, "Task");
        invalidAssignee.setAssigneeId(0L);
        TaskRequest invalidDates = request(3L, "Task");
        invalidDates.setDueDate(LocalDate.of(2025, 12, 31));
        TaskRequest longTitle = request(3L, "a".repeat(151));
        TaskRequest oversizedDescription = request(3L, "Task");
        oversizedDescription.setDescription("\u00e9".repeat(32_768));

        assertThrows(BadRequestException.class, () -> service.createTask(invalidProject, "1"));
        assertThrows(BadRequestException.class, () -> service.createTask(invalidAssignee, "1"));
        assertThrows(BadRequestException.class, () -> service.createTask(invalidDates, "1"));
        assertThrows(BadRequestException.class, () -> service.createTask(longTitle, "1"));
        assertThrows(BadRequestException.class, () -> service.createTask(oversizedDescription, "1"));

        verifyNoInteractions(projects, memberships, users, tasks);
    }

    @Test
    void onlyProjectManagersCanCreateTasks() {
        User requester = user(2L);
        Project project = project(3L);
        when(requesterResolver.resolve("2")).thenReturn(requester);
        when(projects.findById(3L)).thenReturn(Optional.of(project));
        when(memberships.findByProjectIdAndUserId(3L, 2L))
                .thenReturn(Optional.of(membership(project, requester, "MEMBER")));

        assertThrows(ForbiddenException.class,
                () -> service.createTask(request(3L, "Task"), "2"));

        verifyNoInteractions(tasks);
    }

    @Test
    void rejectsAssigneeWhoIsNotAProjectMember() {
        User requester = user(1L);
        User assignee = user(2L);
        Project project = project(3L);
        when(requesterResolver.resolve("1")).thenReturn(requester);
        when(projects.findById(3L)).thenReturn(Optional.of(project));
        when(memberships.findByProjectIdAndUserId(3L, 1L))
                .thenReturn(Optional.of(membership(project, requester, "MANAGER")));
        when(users.findById(2L)).thenReturn(Optional.of(assignee));
        when(memberships.existsByProjectIdAndUserId(3L, 2L)).thenReturn(false);

        assertThrows(BadRequestException.class, () -> {
            TaskRequest request = request(3L, "Task");
            request.setAssigneeId(2L);
            service.createTask(request, "1");
        });

        verify(tasks, never()).save(any(Task.class));
    }

    @Test
    void managerCanSeeEveryTaskInTheirProjectsButNotOtherProjects() {
        User manager = user(1L);
        Project project = project(3L);
        Project otherProject = project(4L);
        Task first = task(project, null);
        Task assignedToAnother = task(project, user(8L));
        Task outsideMembership = task(otherProject, null);
        when(requesterResolver.resolve("1")).thenReturn(manager);
        when(memberships.findAllByUserId(1L))
                .thenReturn(List.of(membership(project, manager, "MANAGER")));
        when(tasks.findByStatus(TaskStatus.TODO))
                .thenReturn(List.of(first, assignedToAnother, outsideMembership));

        List<Task> visible = service.getTasksByStatus(TaskStatus.TODO, "1");

        assertEquals(List.of(first, assignedToAnother), visible);
    }

    @Test
    void memberCannotUseAssigneeFilterToSeeAnotherUsersTask() {
        User requester = user(2L);
        User otherAssignee = user(9L);
        Project project = project(3L);
        Task assignedToOther = task(project, otherAssignee);
        when(requesterResolver.resolve("2")).thenReturn(requester);
        when(memberships.findAllByUserId(2L))
                .thenReturn(List.of(membership(project, requester, "MEMBER")));
        when(tasks.findByAssignee_Id(9L)).thenReturn(List.of(assignedToOther));

        assertTrue(service.getTasksByAssignee(9L, "2").isEmpty());
    }

    @Test
    void memberCanSeeOnlyTheirAssignedTasksAcrossFilterEndpoints() {
        User requester = user(2L);
        Project project = project(3L);
        Task assignedToRequester = task(project, requester);
        Task assignedToOther = task(project, user(9L));
        when(requesterResolver.resolve("2")).thenReturn(requester);
        when(memberships.findAllByUserId(2L))
                .thenReturn(List.of(membership(project, requester, "MEMBER")));
        when(tasks.findAll()).thenReturn(List.of(assignedToRequester, assignedToOther));
        when(tasks.findByStatus(TaskStatus.TODO)).thenReturn(List.of(assignedToOther));
        when(tasks.findByPriority(TaskPriority.HIGH)).thenReturn(List.of(assignedToOther));
        when(tasks.findByDueDate(LocalDate.of(2026, 12, 1)))
                .thenReturn(List.of(assignedToOther));
        when(tasks.findByAssignee_Id(9L)).thenReturn(List.of(assignedToOther));
        when(tasks.findByDueDateBeforeAndStatusNot(any(), eq(TaskStatus.DONE)))
                .thenReturn(List.of(assignedToOther));

        assertEquals(List.of(assignedToRequester), service.getAllTasks("2", null));
        assertTrue(service.getTasksByStatus(TaskStatus.TODO, "2").isEmpty());
        assertTrue(service.getTasksByPriority(TaskPriority.HIGH, "2").isEmpty());
        assertTrue(service.getTasksByDueDate(LocalDate.of(2026, 12, 1), "2").isEmpty());
        assertTrue(service.getTasksByAssignee(9L, "2").isEmpty());
        assertTrue(service.getOverdueTasks("2").isEmpty());
    }

    @Test
    void memberCanViewAssignedTaskButNotUnassignedOrNonmemberTask() {
        User requester = user(2L);
        Project project = project(3L);
        Task assigned = task(project, requester);
        Task unassigned = task(project, null);
        when(requesterResolver.resolve("2")).thenReturn(requester);
        when(tasks.findById(10L)).thenReturn(Optional.of(assigned));
        when(tasks.findById(11L)).thenReturn(Optional.of(unassigned));
        when(tasks.findById(12L)).thenReturn(Optional.of(task(project(4L), requester)));
        when(memberships.findByProjectIdAndUserId(3L, 2L))
                .thenReturn(Optional.of(membership(project, requester, "MEMBER")));

        assertSame(assigned, service.getTaskById(10L, "2"));
        assertThrows(ForbiddenException.class, () -> service.getTaskById(11L, "2"));
        assertThrows(ForbiddenException.class, () -> service.getTaskById(12L, "2"));
    }

    @Test
    void projectTaskListRequiresMembership() {
        User requester = user(2L);
        Project project = project(3L);
        when(requesterResolver.resolve("2")).thenReturn(requester);
        when(projects.findById(3L)).thenReturn(Optional.of(project));
        when(memberships.findByProjectIdAndUserId(3L, 2L)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> service.getTasksByProject(3L, "2"));

        verifyNoInteractions(tasks);
    }

    @Test
    void nonexistentProjectAndTaskKeepReturningNotFound() {
        User requester = user(1L);
        when(requesterResolver.resolve("1")).thenReturn(requester);
        when(projects.findById(3L)).thenReturn(Optional.empty());
        when(tasks.findById(10L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getTasksByProject(3L, "1"));
        assertThrows(NotFoundException.class, () -> service.getTaskById(10L, "1"));
    }

    @Test
    void managerCannotChangeStatusOfTaskAssignedToSomeoneElse() {
        User manager = user(1L);
        Project project = project(3L);
        Task task = task(project, user(2L));
        when(requesterResolver.resolve("1")).thenReturn(manager);
        when(tasks.findById(10L)).thenReturn(Optional.of(task));
        when(projects.findById(3L)).thenReturn(Optional.of(project));
        when(memberships.findByProjectIdAndUserId(3L, 1L))
                .thenReturn(Optional.of(membership(project, manager, "MANAGER")));

        assertThrows(ForbiddenException.class,
                () -> service.updateTaskStatus(10L, TaskStatus.DONE, "1"));
    }

    @Test
    void assignedMemberCanChangeStatusWithoutAdditionalTransitionRestrictions() {
        User assignee = user(2L);
        Project project = project(3L);
        Task task = task(project, assignee);
        when(requesterResolver.resolve("2")).thenReturn(assignee);
        when(tasks.findById(10L)).thenReturn(Optional.of(task));
        when(projects.findById(3L)).thenReturn(Optional.of(project));
        when(memberships.findByProjectIdAndUserId(3L, 2L))
                .thenReturn(Optional.of(membership(project, assignee, "MEMBER")));
        when(tasks.save(task)).thenReturn(task);

        assertSame(task, service.updateTaskStatus(10L, TaskStatus.DONE, "2"));
        assertEquals(TaskStatus.DONE, task.getStatus());
    }

    @Test
    void updateRejectsProjectChangesAndTaskStatusChangesByUnassignedManager() {
        User manager = user(1L);
        Project project = project(3L);
        Task task = task(project, user(2L));
        when(requesterResolver.resolve("1")).thenReturn(manager);
        when(tasks.findById(10L)).thenReturn(Optional.of(task));
        when(projects.findById(3L)).thenReturn(Optional.of(project));
        when(memberships.findByProjectIdAndUserId(3L, 1L))
                .thenReturn(Optional.of(membership(project, manager, "MANAGER")));
        TaskRequest moveRequest = request(4L, "Task");

        assertThrows(BadRequestException.class,
                () -> service.updateTask(10L, moveRequest, "1"));

        TaskRequest statusRequest = new TaskRequest();
        statusRequest.setStatus(TaskStatus.DONE);
        assertThrows(ForbiddenException.class,
                () -> service.updateTask(10L, statusRequest, "1"));
    }

    @Test
    void updateRejectsStartDateAfterExistingDueDate() {
        Task task = managerTaskWithDates(
                LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 20));
        TaskRequest update = new TaskRequest();
        update.setStartDate(LocalDate.of(2026, 3, 21));

        assertThrows(BadRequestException.class,
                () -> service.updateTask(10L, update, "1"));

        verify(tasks, never()).save(any(Task.class));
    }

    @Test
    void updateRejectsDueDateBeforeExistingStartDate() {
        Task task = managerTaskWithDates(
                LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 20));
        TaskRequest update = new TaskRequest();
        update.setDueDate(LocalDate.of(2026, 3, 9));

        assertThrows(BadRequestException.class,
                () -> service.updateTask(10L, update, "1"));

        verify(tasks, never()).save(any(Task.class));
    }

    @Test
    void updateAllowsValidStartDateUsingExistingDueDate() {
        Task task = managerTaskWithDates(
                LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 20));
        TaskRequest update = new TaskRequest();
        update.setStartDate(LocalDate.of(2026, 3, 15));
        when(tasks.save(task)).thenReturn(task);

        assertSame(task, service.updateTask(10L, update, "1"));

        assertEquals(LocalDate.of(2026, 3, 15), task.getStartDate());
        assertEquals(LocalDate.of(2026, 3, 20), task.getDueDate());
    }

    @Test
    void updateAllowsValidDueDateUsingExistingStartDate() {
        Task task = managerTaskWithDates(
                LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 20));
        TaskRequest update = new TaskRequest();
        update.setDueDate(LocalDate.of(2026, 3, 15));
        when(tasks.save(task)).thenReturn(task);

        assertSame(task, service.updateTask(10L, update, "1"));

        assertEquals(LocalDate.of(2026, 3, 10), task.getStartDate());
        assertEquals(LocalDate.of(2026, 3, 15), task.getDueDate());
    }

    @Test
    void updateAllowsBothDatesWhenRangeIsValid() {
        Task task = managerTaskWithDates(
                LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 20));
        TaskRequest update = new TaskRequest();
        update.setStartDate(LocalDate.of(2026, 3, 15));
        update.setDueDate(LocalDate.of(2026, 3, 25));
        when(tasks.save(task)).thenReturn(task);

        assertSame(task, service.updateTask(10L, update, "1"));

        assertEquals(LocalDate.of(2026, 3, 15), task.getStartDate());
        assertEquals(LocalDate.of(2026, 3, 25), task.getDueDate());
    }

    @Test
    void updateRejectsInvalidSuppliedTitleAndAssigneeId() {
        managerTaskWithDates(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 20));
        TaskRequest longTitle = new TaskRequest();
        longTitle.setTitle("a".repeat(151));
        TaskRequest invalidAssignee = new TaskRequest();
        invalidAssignee.setAssigneeId(-1L);

        assertThrows(BadRequestException.class,
                () -> service.updateTask(10L, longTitle, "1"));
        assertThrows(BadRequestException.class,
                () -> service.updateTask(10L, invalidAssignee, "1"));

        verify(tasks, never()).save(any(Task.class));
        verify(users, never()).findById(any());
    }

    @Test
    void managerCanUpdateAndReassignOnlyToProjectMember() {
        User manager = user(1L);
        User newAssignee = user(4L);
        Project project = project(3L);
        Task task = task(project, user(2L));
        when(requesterResolver.resolve("1")).thenReturn(manager);
        when(tasks.findById(10L)).thenReturn(Optional.of(task));
        when(projects.findById(3L)).thenReturn(Optional.of(project));
        when(memberships.findByProjectIdAndUserId(3L, 1L))
                .thenReturn(Optional.of(membership(project, manager, "MANAGER")));
        when(users.findById(4L)).thenReturn(Optional.of(newAssignee));
        when(memberships.existsByProjectIdAndUserId(3L, 4L)).thenReturn(true);
        when(tasks.save(task)).thenReturn(task);
        TaskRequest update = new TaskRequest();
        update.setProjectId(3L);
        update.setTitle("Updated");
        update.setAssigneeId(4L);

        assertSame(task, service.updateTask(10L, update, "1"));
        assertEquals("Updated", task.getTitle());
        assertSame(newAssignee, task.getAssignee());
    }

    @Test
    void managerCannotReassignTaskToNonmember() {
        User manager = user(1L);
        User nonmember = user(5L);
        Project project = project(3L);
        Task task = task(project, user(2L));
        when(requesterResolver.resolve("1")).thenReturn(manager);
        when(tasks.findById(10L)).thenReturn(Optional.of(task));
        when(projects.findById(3L)).thenReturn(Optional.of(project));
        when(memberships.findByProjectIdAndUserId(3L, 1L))
                .thenReturn(Optional.of(membership(project, manager, "MANAGER")));
        when(users.findById(5L)).thenReturn(Optional.of(nonmember));
        when(memberships.existsByProjectIdAndUserId(3L, 5L)).thenReturn(false);
        TaskRequest update = new TaskRequest();
        update.setAssigneeId(5L);

        assertThrows(BadRequestException.class, () -> service.updateTask(10L, update, "1"));
        verify(tasks, never()).save(any(Task.class));
    }

    @Test
    void nonmemberCannotUpdateOrDeleteTask() {
        User requester = user(5L);
        Project project = project(3L);
        Task task = task(project, null);
        when(requesterResolver.resolve("5")).thenReturn(requester);
        when(tasks.findById(10L)).thenReturn(Optional.of(task));
        when(projects.findById(3L)).thenReturn(Optional.of(project));
        when(memberships.findByProjectIdAndUserId(3L, 5L)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class,
                () -> service.updateTask(10L, new TaskRequest(), "5"));
        assertThrows(ForbiddenException.class, () -> service.deleteTask(10L, "5"));
        verify(tasks, never()).save(any(Task.class));
        verify(tasks, never()).delete(any(Task.class));
    }

    @Test
    void projectMemberCannotUpdateOrDeleteTask() {
        User member = user(2L);
        Project project = project(3L);
        Task task = task(project, member);
        when(requesterResolver.resolve("2")).thenReturn(member);
        when(tasks.findById(10L)).thenReturn(Optional.of(task));
        when(projects.findById(3L)).thenReturn(Optional.of(project));
        when(memberships.findByProjectIdAndUserId(3L, 2L))
                .thenReturn(Optional.of(membership(project, member, "MEMBER")));

        assertThrows(ForbiddenException.class,
                () -> service.updateTask(10L, new TaskRequest(), "2"));
        assertThrows(ForbiddenException.class, () -> service.deleteTask(10L, "2"));
        verify(tasks, never()).save(any(Task.class));
        verify(tasks, never()).delete(any(Task.class));
    }

    @Test
    void managerCanDeleteTask() {
        User manager = user(1L);
        Project project = project(3L);
        Task task = task(project, null);
        when(requesterResolver.resolve("1")).thenReturn(manager);
        when(tasks.findById(10L)).thenReturn(Optional.of(task));
        when(projects.findById(3L)).thenReturn(Optional.of(project));
        when(memberships.findByProjectIdAndUserId(3L, 1L))
                .thenReturn(Optional.of(membership(project, manager, "MANAGER")));

        service.deleteTask(10L, "1");

        verify(tasks).delete(task);
    }

    private User user(long id) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Project project(long id) {
        Project project = new Project();
        ReflectionTestUtils.setField(project, "id", id);
        return project;
    }

    private ProjectMember membership(Project project, User user, String role) {
        return new ProjectMember(project, user, role);
    }

    private Task task(Project project, User assignee) {
        Task task = new Task();
        task.setProject(project);
        task.setAssignee(assignee);
        task.setStatus(TaskStatus.TODO);
        return task;
    }

    private Task managerTaskWithDates(LocalDate startDate, LocalDate dueDate) {
        User manager = user(1L);
        Project project = project(3L);
        Task task = task(project, null);
        task.setStartDate(startDate);
        task.setDueDate(dueDate);
        when(requesterResolver.resolve("1")).thenReturn(manager);
        when(tasks.findById(10L)).thenReturn(Optional.of(task));
        when(projects.findById(3L)).thenReturn(Optional.of(project));
        when(memberships.findByProjectIdAndUserId(3L, 1L))
                .thenReturn(Optional.of(membership(project, manager, "MANAGER")));
        return task;
    }

    private TaskRequest request(Long projectId, String title) {
        TaskRequest request = new TaskRequest();
        request.setProjectId(projectId);
        request.setTitle(title);
        request.setStartDate(LocalDate.of(2026, 1, 1));
        return request;
    }
}
