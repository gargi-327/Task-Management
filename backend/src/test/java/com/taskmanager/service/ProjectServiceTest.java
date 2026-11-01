package com.taskmanager.service;

import com.taskmanager.dto.ProjectRequest;
import com.taskmanager.dto.ProjectSummaryResponse;
import com.taskmanager.dto.ProjectUpdateRequest;
import com.taskmanager.entity.Project;
import com.taskmanager.entity.TaskStatus;
import com.taskmanager.entity.User;
import com.taskmanager.exception.BadRequestException;
import com.taskmanager.exception.NotFoundException;
import com.taskmanager.repository.ProjectRepository;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {
    @Mock private ProjectRepository projects;
    @Mock private UserRepository users;
    @Mock private TaskRepository tasks;
    @InjectMocks private ProjectService service;

    @Test
    void rejectsProjectDueDateBeforeStartDate() {
        ProjectRequest request = new ProjectRequest("Website", null,
                LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 1), 7L);
        assertThrows(BadRequestException.class, () -> service.create(request));
        verifyNoInteractions(users, projects);
    }

    @Test
    void rejectsBlankProjectNameAtServiceLayer() {
        ProjectRequest request = new ProjectRequest("   ", null, null, null, 7L);
        assertThrows(BadRequestException.class, () -> service.create(request));
        verifyNoInteractions(users, projects);
    }

    @Test
    void rejectsUnknownCreator() {
        ProjectRequest request = new ProjectRequest("Website", null, null, null, 99L);
        when(users.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.create(request));
        verify(projects, never()).save(any());
    }

    @Test
    void rejectsUnknownProjectDuringArchive() {
        when(projects.findById(404L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.archive(404L));
    }

    @Test
    void archivesBySettingFlagInsteadOfDeleting() {
        User creator = new User();
        Project project = new Project();
        project.setCreatedBy(creator);
        when(projects.findById(1L)).thenReturn(Optional.of(project));
        service.archive(1L);
        org.junit.jupiter.api.Assertions.assertTrue(project.isArchived());
        verify(projects, never()).delete(any());
    }

    @Test
    void updatePreservesCreatorArchiveStatusAndAbsentFields() {
        User creator = new User();
        Project project = new Project();
        project.setName("Original name");
        project.setDescription("Original description");
        project.setStartDate(LocalDate.of(2026, 11, 1));
        project.setDueDate(LocalDate.of(2026, 11, 20));
        project.setCreatedBy(creator);
        project.setArchived(true);
        when(projects.findById(1L)).thenReturn(Optional.of(project));

        service.update(1L, new ProjectUpdateRequest(" Updated name ", null, null, null));

        assertEquals("Updated name", project.getName());
        assertEquals("Original description", project.getDescription());
        assertEquals(LocalDate.of(2026, 11, 1), project.getStartDate());
        assertEquals(LocalDate.of(2026, 11, 20), project.getDueDate());
        assertSame(creator, project.getCreatedBy());
        assertTrue(project.isArchived());
        verifyNoInteractions(users);
    }

    @Test
    void rejectsInvalidEffectiveDateRangeDuringPartialUpdate() {
        Project project = new Project();
        project.setStartDate(LocalDate.of(2026, 11, 10));
        project.setDueDate(LocalDate.of(2026, 11, 20));
        project.setCreatedBy(new User());
        when(projects.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(BadRequestException.class,
                () -> service.update(1L, new ProjectUpdateRequest(null, null, null, LocalDate.of(2026, 11, 1))));
        assertEquals(LocalDate.of(2026, 11, 20), project.getDueDate());
    }

    @Test
    void returnsCorrectProjectSummaryCounts() {
        Project project = new Project();
        project.setCreatedBy(new User());
        when(projects.findById(1L)).thenReturn(Optional.of(project));
        when(tasks.countByProjectId(1L)).thenReturn(8L);
        when(tasks.countByProjectIdAndStatus(1L, TaskStatus.TODO)).thenReturn(3L);
        when(tasks.countByProjectIdAndStatus(1L, TaskStatus.IN_PROGRESS)).thenReturn(2L);
        when(tasks.countByProjectIdAndStatus(1L, TaskStatus.DONE)).thenReturn(3L);
        when(tasks.countOverdueByProjectId(eq(1L), any(LocalDate.class))).thenReturn(1L);

        ProjectSummaryResponse summary = service.summary(1L);

        assertEquals(8L, summary.totalTasks());
        assertEquals(3L, summary.pendingTasks());
        assertEquals(2L, summary.inProgressTasks());
        assertEquals(3L, summary.completedTasks());
        assertEquals(1L, summary.overdueTasks());
        verify(tasks).countOverdueByProjectId(eq(1L), any(LocalDate.class));
    }
}
