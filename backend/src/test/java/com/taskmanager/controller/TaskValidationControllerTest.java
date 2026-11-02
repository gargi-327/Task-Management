package com.taskmanager.controller;

import com.taskmanager.entity.Project;
import com.taskmanager.entity.ProjectMember;
import com.taskmanager.entity.Task;
import com.taskmanager.entity.TaskStatus;
import com.taskmanager.entity.User;
import com.taskmanager.repository.ProjectMemberRepository;
import com.taskmanager.repository.ProjectRepository;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.repository.UserRepository;
import com.taskmanager.service.RequesterResolver;
import com.taskmanager.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@Import({TaskService.class, RequesterResolver.class})
class TaskValidationControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private TaskRepository tasks;
    @MockBean private ProjectRepository projects;
    @MockBean private UserRepository users;
    @MockBean private ProjectMemberRepository memberships;

    @Test
    void rejectsDueDateBeforeStartDateWithBadRequest() throws Exception {
        when(users.findById(1L)).thenReturn(Optional.of(user(1L)));

        mockMvc.perform(post("/api/tasks")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":3,\"title\":\"Prepare release\","
                                + "\"startDate\":\"2026-03-10\",\"dueDate\":\"2026-03-09\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Due date cannot be before start date"));
    }

    @Test
    void rejectsInvalidDateRangeInPartialUpdateWithBadRequest() throws Exception {
        User requester = user(1L);
        Project project = project(3L);
        Task task = new Task();
        task.setProject(project);
        task.setTitle("Prepare release");
        task.setStatus(TaskStatus.TODO);
        task.setStartDate(LocalDate.of(2026, 3, 10));
        task.setDueDate(LocalDate.of(2026, 3, 20));
        when(users.findById(1L)).thenReturn(Optional.of(requester));
        when(tasks.findById(10L)).thenReturn(Optional.of(task));
        when(projects.findById(3L)).thenReturn(Optional.of(project));
        when(memberships.findByProjectIdAndUserId(3L, 1L))
                .thenReturn(Optional.of(new ProjectMember(project, requester, "MANAGER")));

        mockMvc.perform(put("/api/tasks/10")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dueDate\":\"2026-03-09\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Due date cannot be before start date"));
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
}
