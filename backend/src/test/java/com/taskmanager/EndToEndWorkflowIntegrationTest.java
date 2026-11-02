package com.taskmanager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanager.entity.Role;
import com.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.bootstrap.manager.name=Workflow Manager",
        "app.bootstrap.manager.email=workflow-manager@example.test",
        "spring.datasource.url=jdbc:h2:mem:task-management-workflow-${random.uuid};DB_CLOSE_DELAY=-1;MODE=MySQL"
})
@AutoConfigureMockMvc
class EndToEndWorkflowIntegrationTest {
    private static final String MANAGER_EMAIL = "workflow-manager@example.test";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository users;

    @Test
    void completesProjectMembershipTaskAndAuthorizationWorkflow() throws Exception {
        assertEquals(1, users.count(), "The isolated database should contain only the bootstrapped manager");
        long managerId = users.findByEmailIgnoreCase(MANAGER_EMAIL).orElseThrow().getId();
        assertEquals(Role.MANAGER, users.findById(managerId).orElseThrow().getRole());

        assertUnauthorized(get("/api/projects"));
        assertUnauthorized(get("/api/projects").header("X-User-Id", "not-a-number"));

        mockMvc.perform(get("/api/users/{id}", managerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MANAGER"));

        long memberId = createUser(managerId, "Project Member", "workflow-member@example.test");
        long otherMemberId = createUser(managerId, "Other Member", "workflow-other@example.test");
        long nonMemberId = createUser(managerId, "Outside User", "workflow-outside@example.test");

        mockMvc.perform(post("/api/users")
                        .header("X-User-Id", managerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("Duplicate Member", "workflow-member@example.test")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());

        mockMvc.perform(post("/api/projects")
                        .header("X-User-Id", memberId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Workflow Project\",\"description\":\"API integration\","
                                + "\"createdById\":" + memberId + "}"))
                .andExpect(status().isForbidden());

        MvcResult projectCreation = mockMvc.perform(post("/api/projects")
                        .header("X-User-Id", managerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Workflow Project\",\"description\":\"API integration\","
                                + "\"createdById\":" + managerId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdById").value(managerId))
                .andReturn();
        long projectId = idFrom(projectCreation);

        mockMvc.perform(get("/api/projects/{id}", projectId)
                        .header("X-User-Id", managerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectId));
        mockMvc.perform(get("/api/projects/{id}", projectId)
                        .header("X-User-Id", memberId))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/projects/999999")
                        .header("X-User-Id", managerId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
        mockMvc.perform(get("/api/users/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());

        addMember(managerId, projectId, memberId);
        addMember(managerId, projectId, otherMemberId);

        mockMvc.perform(post("/api/projects/{projectId}/members", projectId)
                        .header("X-User-Id", managerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(memberJson(memberId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
        mockMvc.perform(post("/api/projects/{projectId}/members", projectId)
                        .header("X-User-Id", managerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(memberJson(999999L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
        mockMvc.perform(get("/api/projects/{projectId}/members", projectId)
                        .header("X-User-Id", managerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[?(@.userId == " + managerId + ")].membershipRole")
                        .value(org.hamcrest.Matchers.contains("MANAGER")));
        mockMvc.perform(get("/api/projects/{id}", projectId)
                        .header("X-User-Id", memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectId));
        mockMvc.perform(get("/api/projects/{projectId}/members", projectId)
                        .header("X-User-Id", memberId))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/projects/{projectId}/members", projectId)
                        .header("X-User-Id", memberId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(memberJson(nonMemberId)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/tasks")
                        .header("X-User-Id", managerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskJson(projectId, "Invalid dates", memberId,
                                "2026-11-10", "2026-11-09")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Due date cannot be before start date"));
        mockMvc.perform(post("/api/tasks")
                        .header("X-User-Id", managerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskJson(projectId, "Nonmember assignment", nonMemberId,
                                "2026-11-01", "2099-12-31")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Assignee must be a member of the task project"));
        mockMvc.perform(post("/api/tasks")
                        .header("X-User-Id", managerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskJson(999999L, "Missing project", memberId,
                                "2026-11-01", "2099-12-31")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
        mockMvc.perform(post("/api/tasks")
                        .header("X-User-Id", memberId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskJson(projectId, "Member cannot create", memberId,
                                "2026-11-01", "2099-12-31")))
                .andExpect(status().isForbidden());

        MvcResult assignedTaskResult = createTask(managerId, projectId, memberId,
                "Assigned task");
        long assignedTaskId = idFrom(assignedTaskResult);
        long otherTaskId = idFrom(createTask(managerId, projectId, otherMemberId, "Other task"));

        mockMvc.perform(get("/api/tasks")
                        .header("X-User-Id", managerId)
                        .param("projectId", String.valueOf(projectId))
                        .param("status", "TODO")
                        .param("assigneeId", String.valueOf(memberId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(assignedTaskId));
        mockMvc.perform(get("/api/tasks/project/{projectId}", projectId)
                        .header("X-User-Id", managerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(get("/api/tasks/status/TODO").header("X-User-Id", managerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(get("/api/tasks/assignee/{assigneeId}", memberId)
                        .header("X-User-Id", managerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/tasks/{id}", 999999L).header("X-User-Id", managerId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());

        mockMvc.perform(get("/api/tasks")
                        .header("X-User-Id", memberId)
                        .param("projectId", String.valueOf(projectId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(assignedTaskId));
        mockMvc.perform(get("/api/tasks/project/{projectId}", projectId)
                        .header("X-User-Id", memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].assigneeId").value(memberId));
        mockMvc.perform(get("/api/tasks")
                        .header("X-User-Id", memberId)
                        .param("assigneeId", String.valueOf(otherMemberId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/tasks/assignee/{assigneeId}", otherMemberId)
                        .header("X-User-Id", memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/tasks/{id}", otherTaskId).header("X-User-Id", memberId))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/tasks/project/{projectId}", projectId)
                        .header("X-User-Id", nonMemberId))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/tasks/{id}", assignedTaskId)
                        .header("X-User-Id", memberId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Member cannot edit\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/tasks/{id}", assignedTaskId)
                        .header("X-User-Id", memberId))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/tasks/{id}/status", otherTaskId)
                        .header("X-User-Id", memberId)
                        .param("status", "DONE"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/tasks/{id}/status", assignedTaskId)
                        .header("X-User-Id", memberId)
                        .param("status", "IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        mockMvc.perform(get("/api/tasks/{id}", assignedTaskId)
                        .header("X-User-Id", memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        mockMvc.perform(get("/api/projects/{id}/summary", projectId)
                        .header("X-User-Id", managerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTasks").value(2))
                .andExpect(jsonPath("$.todoTasks").value(1))
                .andExpect(jsonPath("$.inProgressTasks").value(1))
                .andExpect(jsonPath("$.completedTasks").value(0));

        mockMvc.perform(delete("/api/projects/{projectId}/members/{userId}", projectId, managerId)
                        .header("X-User-Id", managerId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot remove the last MANAGER from a project"));
        mockMvc.perform(get("/api/projects/{projectId}/members", projectId)
                        .header("X-User-Id", managerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    private long createUser(long managerId, String name, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users")
                        .header("X-User-Id", managerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson(name, email)))
                .andExpect(status().isCreated())
                .andReturn();
        return idFrom(result);
    }

    private void addMember(long managerId, long projectId, long userId) throws Exception {
        mockMvc.perform(post("/api/projects/{projectId}/members", projectId)
                        .header("X-User-Id", managerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(memberJson(userId)))
                .andExpect(status().isCreated());
    }

    private MvcResult createTask(long managerId, long projectId, long assigneeId, String title)
            throws Exception {
        return mockMvc.perform(post("/api/tasks")
                        .header("X-User-Id", managerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskJson(projectId, title, assigneeId,
                                "2026-11-01", "2099-12-31")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assigneeId").value(assigneeId))
                .andReturn();
    }

    private void assertUnauthorized(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        mockMvc.perform(request)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("A valid X-User-Id header is required"));
    }

    private String userJson(String name, String email) {
        return "{\"name\":\"" + name + "\",\"email\":\"" + email + "\",\"role\":\"MEMBER\"}";
    }

    private String memberJson(long userId) {
        return "{\"userId\":" + userId + ",\"membershipRole\":\"MEMBER\"}";
    }

    private String taskJson(
            long projectId,
            String title,
            long assigneeId,
            String startDate,
            String dueDate) {
        return "{\"projectId\":" + projectId
                + ",\"title\":\"" + title
                + "\",\"startDate\":\"" + startDate
                + "\",\"dueDate\":\"" + dueDate
                + "\",\"assigneeId\":" + assigneeId + "}";
    }

    private long idFrom(MvcResult result) throws Exception {
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("id").asLong();
    }
}
