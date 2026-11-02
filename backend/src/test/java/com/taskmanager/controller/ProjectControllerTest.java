package com.taskmanager.controller;

import com.taskmanager.dto.ProjectRequest;
import com.taskmanager.dto.ProjectResponse;
import com.taskmanager.dto.ProjectSummaryResponse;
import com.taskmanager.dto.ProjectUpdateRequest;
import com.taskmanager.exception.NotFoundException;
import com.taskmanager.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;

@WebMvcTest(ProjectController.class)
class ProjectControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private ProjectService projectService;

    private final ProjectResponse project = new ProjectResponse(1L, "Website redesign", "Refresh site",
            LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 20), false, 7L);
    private final String validRequest = """
            {"name":"Website redesign","description":"Refresh site","startDate":"2026-11-01","dueDate":"2026-11-20","createdById":7}
            """;

    @Test
    void createsProject() throws Exception {
        when(projectService.create(any(ProjectRequest.class), eq("7"))).thenReturn(project);
        mockMvc.perform(post("/api/projects").header("X-User-Id", "7")
                        .contentType(MediaType.APPLICATION_JSON).content(validRequest))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.archived").value(false));
    }

    @Test
    void viewsProject() throws Exception {
        when(projectService.findOne(1L, "7")).thenReturn(project);
        mockMvc.perform(get("/api/projects/1").header("X-User-Id", "7"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Website redesign"));
    }

    @Test
    void updatesProject() throws Exception {
        when(projectService.update(eq(1L), any(ProjectRequest.class), eq("7"))).thenReturn(project);
        mockMvc.perform(put("/api/projects/1").header("X-User-Id", "7")
                        .contentType(MediaType.APPLICATION_JSON).content(validRequest))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dueDate").value("2026-11-20"));
    }

    @Test
    void archivesProjectWithoutDeletingIt() throws Exception {
        ProjectResponse archived = new ProjectResponse(1L, "Website redesign", "Refresh site",
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 20), true, 7L);
        when(projectService.archive(1L, "7")).thenReturn(archived);
        mockMvc.perform(patch("/api/projects/1/archive").header("X-User-Id", "7"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.archived").value(true));
    }

    @Test
    void rejectsMissingRequiredProjectFields() throws Exception {
        mockMvc.perform(post("/api/projects").header("X-User-Id", "7")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
    }

    @Test
    void returnsNotFoundForUnknownProjectId() throws Exception {
        when(projectService.findOne(404L, "7")).thenThrow(new NotFoundException("Project 404 was not found"));
        mockMvc.perform(get("/api/projects/404").header("X-User-Id", "7"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Project 404 was not found"));
    }

    @Test
    void listsProjectsForRequester() throws Exception {
        when(projectService.findAll("7", false)).thenReturn(java.util.List.of(project));

        mockMvc.perform(get("/api/projects").header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void returnsSummaryForRequester() throws Exception {
        when(projectService.summary(1L, "7")).thenReturn(
                new ProjectSummaryResponse(1L, 3, 1, 1, 1, 0, LocalDate.of(2026, 11, 1)));

        mockMvc.perform(get("/api/projects/1/summary").header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTasks").value(3));
    }

    @Test
    void partiallyUpdatesProject() throws Exception {
        when(projectService.update(eq(1L), any(ProjectUpdateRequest.class), eq("7")))
                .thenReturn(project);

        mockMvc.perform(patch("/api/projects/1").header("X-User-Id", "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Website redesign\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Website redesign"));
    }

    @Test
    void allowsConfiguredOriginAndRequesterHeaderInCorsPreflight() throws Exception {
        mockMvc.perform(options("/api/projects/1")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5500")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                                "Content-Type, X-User-Id"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "http://localhost:5500"))
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        allOf(containsString("Content-Type"), containsString("X-User-Id"))))
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                        allOf(containsString("GET"), containsString("POST"),
                                containsString("PUT"), containsString("PATCH"),
                                containsString("DELETE"))));
    }
}
