# Backend

Spring Boot REST API starter. The completed project module exposes:

- `POST /api/projects`
- `GET /api/projects?includeArchived=false`
- `GET /api/projects/{id}`
- `PUT /api/projects/{id}`
- `PATCH /api/projects/{id}/archive`

## Run locally

Requires Java 17+, Maven, and MySQL. The application reads its database settings from environment variables; no credentials are stored in the repository.

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/task_manager?createDatabaseIfNotExist=true&serverTimezone=UTC"
$env:DB_USERNAME="task_manager_user"
$env:DB_PASSWORD="your-local-password"
# Optional; defaults to update
$env:JPA_DDL_AUTO="update"
```

```powershell
cd C:\Projects\task_manager\backend
mvn spring-boot:run
```

MySQL is used at runtime. H2 is test-scoped only and is automatically selected by the `test` Spring profile.

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

`createdById` must reference an existing user. User and task endpoints are intentionally the next modules; add a manager user through a seed/configuration step before trying the project create endpoint.

## Project module delivery checklist

- Project entity, repository, service, controller, DTOs, validation, and soft archive support.
- ER diagram and MySQL-style reference schema in `../docs/diagrams/`.
- API contract notes in `../docs/api/api-contract.md`.
- MVC and service tests for create, view, update preservation, archive, summaries, missing required fields, invalid dates, unknown creators, and unknown project IDs.
