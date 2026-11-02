# Backend

Spring Boot REST API starter. The completed project module exposes:

- `POST /api/projects`
- `GET /api/projects?includeArchived=false`
- `GET /api/projects/{id}`
- `PUT /api/projects/{id}`
- `PATCH /api/projects/{id}/archive`

## Run locally

Requires Java 17 and Maven.

```powershell
cd C:\Projects\task_manager\backend
mvn spring-boot:run
```

The default H2 development database is in memory and the H2 console is available at `http://localhost:8080/h2-console`.

Run the project-module test suite:

```powershell
mvn test
```

Create/update request body:

```json
{
  "name": "Website redesign",
  "description": "Refresh the marketing site",
  "startDate": "2026-11-01",
  "dueDate": "2026-11-20",
  "createdById": 1
}
```

`createdById` must match the user identified by `X-User-Id`. The project, task, user, and project-membership endpoints require the requester headers and roles described in `../docs/api/api-contract.md`.

## Initial local/demo manager

The application can create the first global `MANAGER` at startup, but only when the users table is empty. Configure both of these environment variables before starting the application:

- `APP_BOOTSTRAP_MANAGER_NAME`
- `APP_BOOTSTRAP_MANAGER_EMAIL`

They correspond to `app.bootstrap.manager.name` and `app.bootstrap.manager.email`. No default account or credentials are supplied. If both values are absent, bootstrap is skipped with a warning. If only one value is provided, or either value is invalid, startup fails with a clear configuration error.

In PowerShell, set the values for the current terminal and start the application:

```powershell
$env:APP_BOOTSTRAP_MANAGER_NAME = "Local Manager"
$env:APP_BOOTSTRAP_MANAGER_EMAIL = "manager@example.test"
.\mvnw.cmd spring-boot:run
```

After startup, find the new account's ID in the application log entry `Created initial demo MANAGER account with user ID ...` or in `GET /api/users`. Send that ID as the `X-User-Id` header on protected requests.

This bootstrap is intended only for local/demo setup. It does not set a password, provide authentication, or protect against an API caller claiming another user's ID. `X-User-Id` is a demo-only identity lookup and is not secure production authentication. Do not expose this setup to untrusted clients.

## Project module delivery checklist

- Project entity, repository, service, controller, DTOs, validation, and soft archive support.
- ER diagram and MySQL-style reference schema in `../docs/diagrams/`.
- API contract notes in `../docs/api/api-contract.md`.
- MVC and service tests for create, view, update, archive, missing required fields, invalid dates, unknown creators, and unknown project IDs.
