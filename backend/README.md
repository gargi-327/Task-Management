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
