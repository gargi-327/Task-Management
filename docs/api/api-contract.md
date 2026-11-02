# Initial API Contract

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/projects` | Create project (global manager only) |
| GET | `/api/projects` | List permitted, active projects |
| GET | `/api/projects/{id}` | Get project details (project members) |
| PUT | `/api/projects/{id}` | Update project (project manager only) |
| PATCH | `/api/projects/{id}` | Partially update project (project manager only) |
| PATCH | `/api/projects/{id}/archive` | Archive project (project manager only) |
| POST | `/api/tasks` | Create a task (project manager only) |
| GET | `/api/tasks` | List tasks; supports `projectId`, `status`, `priority`, `assigneeId`, and `dueDate` filters |
| GET | `/api/tasks/{id}` | Get one task subject to project membership and task visibility |
| GET | `/api/tasks/project/{projectId}` | List visible tasks in a project |
| GET | `/api/tasks/status/{status}` | List visible tasks by status |
| GET | `/api/tasks/priority/{priority}` | List visible tasks by priority |
| GET | `/api/tasks/due-date/{dueDate}` | List visible tasks by due date (`YYYY-MM-DD`) |
| GET | `/api/tasks/assignee/{assigneeId}` | List visible tasks assigned to a user |
| GET | `/api/tasks/overdue` | List visible overdue tasks |
| PUT | `/api/tasks/{id}` | Update a task (project manager only) |
| PATCH | `/api/tasks/{id}/status` | Change status (assigned user only) |
| DELETE | `/api/tasks/{id}` | Delete a task (project manager only) |
| GET | `/api/projects/{id}/summary` | Get total, TODO/pending, in-progress, done/completed, and overdue counts (project members) |

Task creation requires `projectId`, `title`, and `startDate`. `description`, `priority`, `status`, `dueDate`, and `assigneeId` are optional; priority defaults to `MEDIUM` and status defaults to `TODO`. A due date, when supplied, must not be before the start date. An assignee must already be a member of the task's project; assigning a nonmember returns `400 Bad Request`.

Every task endpoint requires `X-User-Id`. The value is resolved against the database; a missing, malformed, non-positive, or unknown ID returns `401 Unauthorized`. Project members can access tasks only in projects they belong to. A project `MANAGER` can view all tasks in that project; a project `MEMBER` can view only tasks assigned to them. These visibility rules apply to individual tasks and every list/filter route, including `assigneeId`; a filter cannot grant additional access. Nonmembers and members attempting manager-only operations receive `403 Forbidden`, while nonexistent tasks or projects return `404 Not Found`.

Only project `MANAGER` members can create, update, reassign, or delete tasks. On creation, the task creator is always the user resolved from `X-User-Id`; the legacy `createdBy` request field is ignored. Updating a task cannot move it to another project: a different `projectId` returns `400 Bad Request`. Only the user assigned to a task can change its status. Existing status transitions are unchanged. Invalid task IDs and task input return `400 Bad Request`; an unknown task assignee returns `404 Not Found`.

## Project endpoints implemented by Member 1

- `POST /api/projects` returns `201 Created`. It rejects missing names, missing creators, unknown creators, and a due date earlier than the start date.
- `GET /api/projects` lists active projects by default. Use `?includeArchived=true` to include archived projects.
- `GET /api/projects/{id}` returns one project or `404`.
- `PUT /api/projects/{id}` validates and updates project details.
- `PATCH /api/projects/{id}/archive` soft-archives the project by setting `archived=true`; it does not delete project or task history.
- `GET /api/projects/{id}/summary` returns `totalTasks`, `pendingTasks` (`TODO`), `inProgressTasks`, `completedTasks` (`DONE`), and `overdueTasks`. Overdue means `dueDate` before today and a status other than `DONE`.

## Project membership endpoints

- `POST /api/projects/{projectId}/members` adds an existing user to an existing project. The JSON body requires a positive `userId` and a `membershipRole` of `MANAGER` or `MEMBER`. It returns `201 Created` with the member's `userId`, `name`, `email`, and `membershipRole`.
- `GET /api/projects/{projectId}/members` returns those member fields for all project members.
- `DELETE /api/projects/{projectId}/members/{userId}` removes that user from the project and returns `204 No Content`.
- Invalid IDs or request fields return `400`; missing projects, users, or memberships return `404`; duplicate membership returns `409`.

Example request:

```json
{
  "userId": 12,
  "membershipRole": "MEMBER"
}
```

All project and project-membership endpoints require the `X-User-Id` header. The ID is resolved against the database; a missing, malformed, non-positive, or unknown ID returns `401 Unauthorized`. Project membership operations require the requester to have the `MANAGER` membership role in that project. A member can view the project's details and summary, while a nonmember receives `403 Forbidden`. Project lists include only projects the requester belongs to; `includeArchived=true` includes archived projects among those memberships.

Creating a project requires a database-loaded global `MANAGER`; a global `MEMBER` receives `403 Forbidden`. The requester identified by `X-User-Id` becomes the project creator and is added as a project `MANAGER`. The existing `createdById` request field is retained for compatibility and must match the requester ID; a mismatch returns `400 Bad Request`. Updating or archiving a project requires a project `MANAGER` membership; project members and nonmembers receive `403 Forbidden`. Missing projects return `404 Not Found`.

`X-User-Id` is only a database user lookup mechanism, not authentication. A caller can spoof another user's ID until a trusted authentication mechanism is introduced. The `userId` in a membership request identifies the user being added, not the requester.

## User endpoints

- `POST /api/users` creates a user from `name`, `email`, and `role`, returning `201 Created` with `id`, `name`, `email`, and `role`.
- `GET /api/users` returns all users with those response fields.
- `GET /api/users/{id}` returns one user or `404 Not Found`.
- `name` and `email` are required; email must be valid and no longer than 255 characters. `role` is required and must be `MANAGER` or `MEMBER`.
- Email addresses are trimmed and stored in lowercase. A duplicate email (compared without regard to case) returns `409 Conflict`; invalid request data returns `400 Bad Request`.

Example create request:

```json
{
  "name": "Avery",
  "email": "avery@example.com",
  "role": "MEMBER"
}
```

`POST /api/users` requires the `X-User-Id` header to identify an existing user whose database-backed global role is `MANAGER`; otherwise it returns `401 Unauthorized` for a missing, invalid, or unknown requester ID, or `403 Forbidden` for a global `MEMBER`. The header is only a user lookup mechanism and does not authenticate the caller; it can be spoofed until a trusted authentication mechanism is introduced. `GET /api/users` and `GET /api/users/{id}` remain unrestricted and do not require the header.
