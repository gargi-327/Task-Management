# Initial API Contract

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/projects` | Create project (manager only) |
| GET | `/api/projects` | List permitted, active projects |
| GET | `/api/projects/{id}` | Get project details |
| PUT | `/api/projects/{id}` | Update project (manager only) |
| PATCH | `/api/projects/{id}/archive` | Archive project (manager only) |
| POST | `/api/projects/{id}/tasks` | Create a task in a valid project |
| GET | `/api/tasks` | Filter by `projectId`, `status`, `priority`, `assigneeId`, `dueDate` |
| PATCH | `/api/tasks/{id}/status` | Update status subject to ownership and transition rules |
| GET | `/api/projects/{id}/summary` | Get total, TODO/pending, in-progress, done/completed, and overdue counts |

Required task fields: `projectId`, `title`, `priority`, `status`, and `startDate`. `dueDate` and `assigneeId` may be omitted; when a due date is supplied, it must be on or after `startDate`.

## Project endpoints implemented by Member 1

- `POST /api/projects` returns `201 Created`. It rejects missing names, missing creators, unknown creators, and a due date earlier than the start date.
- `GET /api/projects` lists active projects by default. Use `?includeArchived=true` to include archived projects.
- `GET /api/projects/{id}` returns one project or `404`.
- `PUT /api/projects/{id}` is a safe partial update: omitted fields retain their existing values. Creator and archive fields are not accepted by this endpoint.
- `PATCH /api/projects/{id}/archive` soft-archives the project by setting `archived=true`; it does not delete project or task history.
- `GET /api/projects/{id}/summary` returns `totalTasks`, `pendingTasks` (`TODO`), `inProgressTasks`, `completedTasks` (`DONE`), and `overdueTasks`. Overdue means `dueDate` before today and a status other than `DONE`.

Archived projects remain available by ID and in summaries for historical reporting, while default project lists omit them. The task module must refuse new task creation for archived projects and must not delete their existing tasks.
