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
| GET | `/api/projects/{id}/summary` | Get total, TODO, in-progress, done, and overdue counts |

Required task fields: `projectId`, `title`, `priority`, `status`, `startDate`, `dueDate`, and `assigneeId`. `dueDate` must be on or after `startDate`.
