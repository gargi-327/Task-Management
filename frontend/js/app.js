
let projects = [];
let tasks = [];

const projectForm = document.getElementById("projectForm");
const taskForm = document.getElementById("taskForm");

const projectList = document.getElementById("projectList");
const taskList = document.getElementById("taskList");

const projectMessage = document.getElementById("projectMessage");
const taskMessage = document.getElementById("taskMessage");

const taskProject = document.getElementById("taskProject");
const statusFilter = document.getElementById("statusFilter");

function showMessage(element, message, isError = false) {
    element.textContent = message;
    element.style.color = isError ? "#c0392b" : "#218838";
}

function getProjectId(project) {
    return project.id ?? project.projectId;
}

function getTaskId(task) {
    return task.id ?? task.taskId;
}

function getTaskStatus(task) {
    return String(task.status || "PENDING").toUpperCase();
}

function getTaskProjectId(task) {
    return task.projectId ?? task.project?.id;
}

function updateDashboard() {
    document.getElementById("projectCount").textContent = projects.length;
    document.getElementById("taskCount").textContent = tasks.length;

    const completed = tasks.filter(
        task => getTaskStatus(task) === "COMPLETED"
    ).length;

    document.getElementById("completedCount").textContent = completed;
    document.getElementById("pendingCount").textContent =
        tasks.length - completed;
}

function renderProjects() {
    projectList.innerHTML = "";
    taskProject.innerHTML = '<option value="">Select project</option>';

    if (projects.length === 0) {
        projectList.innerHTML =
            '<p class="empty-message">No projects available. Create your first project.</p>';
        return;
    }

    projects.forEach(project => {
        const id = getProjectId(project);

        const card = document.createElement("div");
        card.className = "project-card";

        const heading = document.createElement("h3");
        heading.textContent = project.name || "Untitled Project";

        const description = document.createElement("p");
        description.textContent =
            project.description || "No description provided.";

        const projectIdText = document.createElement("p");
        projectIdText.className = "project-id";
        projectIdText.textContent = `Project ID: ${id ?? "Not provided"}`;

        card.append(heading, description, projectIdText);
        projectList.appendChild(card);

        if (id !== undefined && id !== null) {
            const option = document.createElement("option");
            option.value = id;
            option.textContent = project.name || `Project ${id}`;
            taskProject.appendChild(option);
        }
    });
}

function renderTasks() {
    taskList.innerHTML = "";

    const selectedStatus = statusFilter.value;

    const filteredTasks = tasks.filter(task => {
        return selectedStatus === "ALL" ||
            getTaskStatus(task) === selectedStatus;
    });

    if (filteredTasks.length === 0) {
        taskList.innerHTML =
            '<p class="empty-message">No tasks found.</p>';
        return;
    }

    filteredTasks.forEach(task => {
        const id = getTaskId(task);
        const status = getTaskStatus(task);

        const card = document.createElement("div");
        card.className = "task-card";

        const top = document.createElement("div");
        top.className = "task-top";

        const details = document.createElement("div");

        const heading = document.createElement("h3");
        heading.textContent = task.title || task.name || "Untitled Task";

        const description = document.createElement("p");
        description.textContent =
            task.description || "No description provided.";

        details.append(heading, description);

        const statusLabel = document.createElement("span");
        statusLabel.className = "status";
        statusLabel.textContent = status.replaceAll("_", " ");

        top.append(details, statusLabel);

        const actions = document.createElement("div");
        actions.className = "task-actions";

        const statusSelect = document.createElement("select");

        const statusOptions = [
            ["PENDING", "Pending"],
            ["IN_PROGRESS", "In Progress"],
            ["COMPLETED", "Completed"]
        ];

        statusOptions.forEach(([value, label]) => {
            const option = document.createElement("option");
            option.value = value;
            option.textContent = label;
            statusSelect.appendChild(option);
        });

        statusSelect.value = status;

        const updateButton = document.createElement("button");
        updateButton.textContent = "Update Status";

        updateButton.addEventListener("click", async () => {
            if (id === undefined || id === null) {
                showMessage(taskMessage, "Task ID is missing.", true);
                return;
            }

            updateButton.disabled = true;

            try {
                await API.updateTaskStatus(id, statusSelect.value);
                showMessage(taskMessage, "Task status updated.");
                await loadTasks();
            } catch (error) {
                showMessage(taskMessage, error.message, true);
            } finally {
                updateButton.disabled = false;
            }
        });

        actions.append(statusSelect, updateButton);
        card.append(top, actions);
        taskList.appendChild(card);
    });
}

async function loadProjects() {
    try {
        const data = await API.getProjects();
        projects = Array.isArray(data) ? data : data?.content || [];
        renderProjects();
        updateDashboard();
    } catch (error) {
        showMessage(
            projectMessage,
            "Could not load projects: " + error.message,
            true
        );
    }
}

async function loadTasks() {
    try {
        const data = await API.getTasks();
        tasks = Array.isArray(data) ? data : data?.content || [];
        renderTasks();
        updateDashboard();
    } catch (error) {
        showMessage(
            taskMessage,
            "Could not load tasks: " + error.message,
            true
        );
    }
}

projectForm.addEventListener("submit", async event => {
    event.preventDefault();

    const name = document.getElementById("projectName").value.trim();
    const description =
        document.getElementById("projectDescription").value.trim();

    if (!name) {
        showMessage(projectMessage, "Enter a project name.", true);
        return;
    }

    const submitButton = projectForm.querySelector("button");
    submitButton.disabled = true;

    try {
        await API.createProject({
            name: name,
            description: description
        });

        projectForm.reset();
        showMessage(projectMessage, "Project created successfully.");

        await loadProjects();
    } catch (error) {
        showMessage(projectMessage, error.message, true);
    } finally {
        submitButton.disabled = false;
    }
});

taskForm.addEventListener("submit", async event => {
    event.preventDefault();

    const title = document.getElementById("taskTitle").value.trim();
    const description =
        document.getElementById("taskDescription").value.trim();
    const projectId = taskProject.value;
    const status = document.getElementById("taskStatus").value;

    if (!title || !projectId) {
        showMessage(
            taskMessage,
            "Enter a task title and select a project.",
            true
        );
        return;
    }

    const submitButton = taskForm.querySelector("button");
    submitButton.disabled = true;

    try {
        await API.createTask(projectId, {
            title: title,
            description: description,
            status: status
        });

        taskForm.reset();
        showMessage(taskMessage, "Task created successfully.");

        await loadTasks();
    } catch (error) {
        showMessage(taskMessage, error.message, true);
    } finally {
        submitButton.disabled = false;
    }
});

statusFilter.addEventListener("change", renderTasks);

document.getElementById("refreshBtn").addEventListener("click", async () => {
    await loadProjects();
    await loadTasks();
});

async function startApp() {
    await loadProjects();
    await loadTasks();
}

startApp();
