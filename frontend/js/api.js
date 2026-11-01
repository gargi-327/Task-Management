// Configure this to match the Spring Boot server during development.
/*const API_BASE_URL = "http://localhost:8080/api";

async function apiRequest(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: { "Content-Type": "application/json", ...(options.headers || {}) },
    ...options,
  });
  const body = response.status === 204 ? null : await response.json();
  if (!response.ok) throw new Error(body?.message || "Request failed");
  return body;
}*/




const API_BASE_URL = "http://localhost:8080/api";

async function apiRequest(endpoint, options = {}) {
    const response = await fetch(API_BASE_URL + endpoint, {
        ...options,
        headers: {
            "Content-Type": "application/json",
            ...options.headers
        }
    });

    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || `Request failed: ${response.status}`);
    }

    if (response.status === 204) {
        return null;
    }

    const contentType = response.headers.get("content-type");

    if (contentType && contentType.includes("application/json")) {
        return response.json();
    }

    return null;
}

const API = {
    getProjects() {
        return apiRequest("/projects");
    },

    getProject(id) {
        return apiRequest(`/projects/${encodeURIComponent(id)}`);
    },

    createProject(project) {
        return apiRequest("/projects", {
            method: "POST",
            body: JSON.stringify(project)
        });
    },

    updateProject(id, project) {
        return apiRequest(`/projects/${encodeURIComponent(id)}`, {
            method: "PUT",
            body: JSON.stringify(project)
        });
    },

    getTasks() {
        return apiRequest("/tasks");
    },

    createTask(projectId, task) {
        return apiRequest(
            `/projects/${encodeURIComponent(projectId)}/tasks`,
            {
                method: "POST",
                body: JSON.stringify(task)
            }
        );
    },

    updateTaskStatus(id, status) {
        return apiRequest(
            `/tasks/${encodeURIComponent(id)}/status`,
            {
                method: "PATCH",
                body: JSON.stringify({ status: status })
            }
        );
    },

    getProjectSummary(id) {
        return apiRequest(`/projects/${encodeURIComponent(id)}/summary`);
    }
};

