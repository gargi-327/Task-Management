package com.taskmanager.controller;

import com.taskmanager.dto.TaskRequest;
import com.taskmanager.entity.Task;
import com.taskmanager.entity.TaskPriority;
import com.taskmanager.entity.TaskStatus;
import com.taskmanager.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
class TaskControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private TaskService taskService;

    @Test
    void forwardsRequesterHeaderToEveryTaskEndpoint() throws Exception {
        Task task = new Task();
        task.setTitle("Prepare release");
        when(taskService.createTask(any(TaskRequest.class), eq("12"))).thenReturn(task);
        when(taskService.getAllTasks("12", null)).thenReturn(List.of());
        when(taskService.getTaskById(3L, "12")).thenReturn(task);
        when(taskService.getTasksByProject(4L, "12")).thenReturn(List.of());
        when(taskService.getTasksByStatus(TaskStatus.TODO, "12")).thenReturn(List.of());
        when(taskService.getTasksByPriority(TaskPriority.HIGH, "12")).thenReturn(List.of());
        when(taskService.getTasksByDueDate(LocalDate.of(2026, 11, 2), "12"))
                .thenReturn(List.of());
        when(taskService.getTasksByAssignee(8L, "12")).thenReturn(List.of());
        when(taskService.getOverdueTasks("12")).thenReturn(List.of());
        when(taskService.updateTask(eq(3L), any(TaskRequest.class), eq("12"))).thenReturn(task);
        when(taskService.updateTaskStatus(3L, TaskStatus.DONE, "12")).thenReturn(task);

        mockMvc.perform(post("/api/tasks")
                        .header("X-User-Id", "12")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":4,\"title\":\"Prepare release\","
                                + "\"startDate\":\"2026-11-02\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/tasks").header("X-User-Id", "12"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/tasks/3").header("X-User-Id", "12"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/tasks/project/4").header("X-User-Id", "12"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/tasks/status/TODO").header("X-User-Id", "12"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/tasks/priority/HIGH").header("X-User-Id", "12"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/tasks/due-date/2026-11-02").header("X-User-Id", "12"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/tasks/assignee/8").header("X-User-Id", "12"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/tasks/overdue").header("X-User-Id", "12"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/tasks/3")
                        .header("X-User-Id", "12")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Prepare release\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/tasks/3/status")
                        .header("X-User-Id", "12")
                        .param("status", "DONE"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/tasks/3").header("X-User-Id", "12"))
                .andExpect(status().isNoContent());

        verify(taskService).createTask(any(TaskRequest.class), eq("12"));
        verify(taskService).getAllTasks("12", null);
        verify(taskService).getTaskById(3L, "12");
        verify(taskService).getTasksByProject(4L, "12");
        verify(taskService).getTasksByStatus(TaskStatus.TODO, "12");
        verify(taskService).getTasksByPriority(TaskPriority.HIGH, "12");
        verify(taskService).getTasksByDueDate(LocalDate.of(2026, 11, 2), "12");
        verify(taskService).getTasksByAssignee(8L, "12");
        verify(taskService).getOverdueTasks("12");
        verify(taskService).updateTask(eq(3L), any(TaskRequest.class), eq("12"));
        verify(taskService).updateTaskStatus(3L, TaskStatus.DONE, "12");
        verify(taskService).deleteTask(3L, "12");
    }

    @Test
    void rejectsMissingTaskTitle() throws Exception {
        assertInvalidCreate("{\"projectId\":4,\"startDate\":\"2026-11-02\"}");
    }

    @Test
    void rejectsBlankTaskTitle() throws Exception {
        assertInvalidCreate("{\"projectId\":4,\"title\":\"   \","
                + "\"startDate\":\"2026-11-02\"}");
    }

    @Test
    void rejectsTaskTitleLongerThan150Characters() throws Exception {
        String title = "a".repeat(151);
        assertInvalidCreate("{\"projectId\":4,\"title\":\"" + title
                + "\",\"startDate\":\"2026-11-02\"}");
    }

    @Test
    void rejectsMissingProjectId() throws Exception {
        assertInvalidCreate("{\"title\":\"Prepare release\","
                + "\"startDate\":\"2026-11-02\"}");
    }

    @Test
    void rejectsMissingStartDate() throws Exception {
        assertInvalidCreate("{\"projectId\":4,\"title\":\"Prepare release\"}");
    }

    @Test
    void rejectsNonpositiveAssigneeId() throws Exception {
        assertInvalidCreate("{\"projectId\":4,\"title\":\"Prepare release\","
                + "\"startDate\":\"2026-11-02\",\"assigneeId\":0}");
    }

    @Test
    void acceptsValidPartialUpdateWithoutRequiringCreationFields() throws Exception {
        Task task = new Task();
        task.setTitle("Updated title");
        when(taskService.updateTask(eq(3L), any(TaskRequest.class), eq("12"))).thenReturn(task);

        mockMvc.perform(put("/api/tasks/3")
                        .header("X-User-Id", "12")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated title\"}"))
                .andExpect(status().isOk());

        verify(taskService).updateTask(eq(3L), any(TaskRequest.class), eq("12"));
    }

    @Test
    void rejectsInvalidPartialUpdateValues() throws Exception {
        mockMvc.perform(put("/api/tasks/3")
                        .header("X-User-Id", "12")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\":-1}"))
                .andExpect(status().isBadRequest());

        verify(taskService, org.mockito.Mockito.never())
                .updateTask(eq(3L), any(TaskRequest.class), eq("12"));
    }

    @Test
    void forwardsMissingRequesterForServiceToReject() throws Exception {
        when(taskService.getAllTasks(null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk());

        verify(taskService).getAllTasks(null, null);
    }

    private void assertInvalidCreate(String requestBody) throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .header("X-User-Id", "12")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(taskService, org.mockito.Mockito.never())
                .createTask(any(TaskRequest.class), eq("12"));
    }
}
