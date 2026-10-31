package com.taskmanager.controller;

import com.taskmanager.dto.ProjectResponse;
import com.taskmanager.exception.NotFoundException;
import com.taskmanager.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
        when(projectService.create(any())).thenReturn(project);
        mockMvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON).content(validRequest))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.archived").value(false));
    }

    @Test
    void viewsProject() throws Exception {
        when(projectService.findOne(1L)).thenReturn(project);
        mockMvc.perform(get("/api/projects/1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Website redesign"));
    }

    @Test
    void updatesProject() throws Exception {
        when(projectService.update(eq(1L), any())).thenReturn(project);
        mockMvc.perform(put("/api/projects/1").contentType(MediaType.APPLICATION_JSON).content(validRequest))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dueDate").value("2026-11-20"));
    }

    @Test
    void archivesProjectWithoutDeletingIt() throws Exception {
        ProjectResponse archived = new ProjectResponse(1L, "Website redesign", "Refresh site",
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 20), true, 7L);
        when(projectService.archive(1L)).thenReturn(archived);
        mockMvc.perform(patch("/api/projects/1/archive"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.archived").value(true));
    }

    @Test
    void rejectsMissingRequiredProjectFields() throws Exception {
        mockMvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
    }

    @Test
    void returnsNotFoundForUnknownProjectId() throws Exception {
        when(projectService.findOne(404L)).thenThrow(new NotFoundException("Project 404 was not found"));
        mockMvc.perform(get("/api/projects/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Project 404 was not found"));
    }
}
