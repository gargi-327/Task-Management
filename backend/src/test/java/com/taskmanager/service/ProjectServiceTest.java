package com.taskmanager.service;

import com.taskmanager.dto.ProjectRequest;
import com.taskmanager.entity.Project;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
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
}
