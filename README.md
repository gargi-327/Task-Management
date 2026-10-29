# Task Manager

Starter structure for a Java Spring Boot REST API with a separate HTML, CSS, and vanilla JavaScript frontend.

## Structure

- `backend/` - Spring Boot application, configuration, and tests.
- `frontend/` - standalone pages, CSS, and API client JavaScript.
- `docs/` - ER diagram, API contract, Postman collection, and task checklist PDF.
- `docker/` - optional container deployment files.
- `scripts/` - local setup or database helper scripts.

## Initial decisions to agree before coding

- API base path: `/api`
- Roles: `MANAGER`, `MEMBER`
- Task priorities: `LOW`, `MEDIUM`, `HIGH`
- Task statuses: `TODO`, `IN_PROGRESS`, `DONE`
- Dates: ISO format `YYYY-MM-DD`
- Status flow: `TODO -> IN_PROGRESS -> DONE`; reversal to `TODO` should be explicitly decided.

See `docs/Task_Manager_Implementation_Checklist.pdf` for the team work plan.
