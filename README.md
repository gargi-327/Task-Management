# Task and Project Management Tool

A Java-based web application designed to simplify project organization, task assignment, and progress tracking. The application provides a centralized platform where users can manage projects, organize tasks, track deadlines, and monitor task statuses.

Built using **Java, Spring Boot, Spring Data JPA, Hibernate, and H2 Database**, the application follows a layered architecture and exposes RESTful APIs for communication between the frontend and backend.

---

## Table of Contents

- [Problem Statement](#problem-statement)
- [Objectives](#objectives)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [System Architecture](#system-architecture)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Installation and Setup](#installation-and-setup)
- [Database Configuration](#database-configuration)
- [API Testing](#api-testing)
- [Testing](#testing)
- [Future Enhancements](#future-enhancements)
- [Team Members](#team-members)

---

## Problem Statement

Managing multiple projects and tasks can become challenging when responsibilities, deadlines, and progress are not tracked systematically. Poor coordination and scattered information can result in missed deadlines, unclear ownership, and reduced productivity.

The Task and Project Management Tool addresses these challenges by providing a centralized application for managing projects and their associated tasks. It helps organize work, assign responsibilities, monitor task progress, and maintain project information efficiently.

## Objectives

- Provide a centralized platform for project and task management.
- Support project creation, retrieval, updating, and archiving.
- Enable task creation and assignment to team members.
- Maintain task priorities, statuses, start dates, and deadlines.
- Support filtering tasks using relevant criteria.
- Validate user input and maintain consistent records.
- Provide REST APIs for communication between the frontend and backend.
- Store project and task information persistently.

## Features

### 1. Project Management
- Create new projects.
- Retrieve existing projects.
- Update project details.
- Archive projects.

### 2. Task Management
- Create tasks associated with projects.
- Assign tasks to team members.
- Store task titles and descriptions.
- Maintain task priorities and statuses.
- Record start dates and due dates.

### 3. Task Tracking
- Filter tasks by project, status, priority, assignee, and due date.
- Track task progress.
- Identify overdue tasks where supported.

### 4. Input Validation
- Validate required fields.
- Check date constraints.
- Handle invalid requests with appropriate error responses.

### 5. Database Persistence
- Store project and task records in an H2 database.
- Retrieve stored records through backend APIs.
- Maintain data between application restarts using file-based database storage.

---

## Technology Stack

| Technology | Purpose |
|---|---|
| Java 17 | Core programming language |
| Spring Boot 3.4.4 | Backend application framework |
| Spring Data JPA | Database access and persistence |
| Hibernate | Object-relational mapping |
| H2 Database | Persistent data storage |
| HTML5 | Frontend structure |
| CSS3 | Frontend styling |
| JavaScript | Frontend interactions and API communication |
| Apache Maven | Build and dependency management |
| Postman | REST API testing |
| Git | Version control |
| GitHub | Source code hosting and collaboration |

---

## System Architecture

The application follows a layered architecture that separates user interaction, business logic, and data access.

```text
                  USER
                    |
                    v
             FRONTEND LAYER
          HTML / CSS / JavaScript
                    |
                    v
               REST APIs
                    |
                    v
            SPRING BOOT BACKEND
                    |
                    v
            CONTROLLER LAYER
                    |
                    v
              SERVICE LAYER
                    |
                    v
             REPOSITORY LAYER
                    |
                    v
               H2 DATABASE
```

### Architecture Components

- **Frontend Layer:** Provides the interface for interacting with projects and tasks.
- **Controller Layer:** Receives HTTP requests and returns appropriate responses.
- **Service Layer:** Handles application logic and business rules.
- **Repository Layer:** Performs database operations using Spring Data JPA.
- **Entity Layer:** Represents project and task data.
- **Database Layer:** Stores records using the H2 database.

---

## Project Structure

The backend is organized using a standard Spring Boot project structure.

```text
task_manager/
├── backend/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/
│   │       │       └── taskmanager/
│   │       │           ├── controller/
│   │       │           ├── service/
│   │       │           ├── repository/
│   │       │           ├── entity/
│   │       │           └── dto/
│   │       └── resources/
│   │           └── application.properties
│   ├── pom.xml
│   └── README.md
└── frontend/
    ├── index.html
    ├── style.css
    └── script.js
```

*The directory tree illustrates the intended organization of the application. Actual package and frontend filenames may differ.*

---

## Prerequisites

Ensure the following software is installed:

1. Java Development Kit (JDK) 17 or later, with Java 17 used by the project.
2. Apache Maven or the Maven Wrapper included with the project.
3. Git for cloning the repository.
4. Postman for testing REST APIs.
5. A modern web browser for accessing the frontend.

Verify the Java installation:

```bash
java -version
```

Verify Maven:

```bash
mvn -version
```

---

## Installation and Setup

### Step 1: Clone the Repository

Clone the project using Git:

```bash
git clone https://github.com/gargi-327/Task-Management
```

Navigate to the backend directory:

```bash
cd task_manager/backend
```

Replace `<YOUR_GITHUB_REPOSITORY_URL>` with the repository URL provided in the repository section below.

### Step 2: Configure the Database

The application uses a file-based H2 database. Its configuration is maintained in:

```text
src/main/resources/application.properties
```

The database configuration is:

```properties
spring.application.name=task-manager-api

spring.datasource.url=jdbc:h2:file:./data/taskmanager
spring.datasource.username=sa
spring.datasource.password=
spring.datasource.driver-class-name=org.h2.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false

spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

app.cors.allowed-origin=http://localhost:5500
```

The database stores records locally and supports persistence across application restarts.

### Step 3: Build the Backend

From the backend directory, execute:

```bash
mvn clean install
```

If Maven is not installed globally and the repository includes the Maven Wrapper, use the following command on Windows:

```powershell
.\mvnw.cmd clean install
```

### Step 4: Run the Backend

Start the Spring Boot application:

```bash
mvn spring-boot:run
```

Alternatively, on Windows, use the Maven Wrapper:

```powershell
.\mvnw.cmd spring-boot:run
```

Once started, the backend is available at:

**http://localhost:8080**

Keep the terminal running while using the application.

### Step 5: Run the Frontend

1. Open the frontend folder in Visual Studio Code or another editor.
2. Start the frontend using a local development server.
3. If using Visual Studio Code Live Server, open the frontend's HTML file and select **Open with Live Server**.
4. Ensure the frontend is served from `http://localhost:5500` to match the configured CORS origin.
5. Verify that the frontend sends requests to the running backend at `http://localhost:8080`.

### Step 6: Access the H2 Database Console

Open the following address in your browser:

**http://localhost:8080/h2-console**

Use the following connection details:

| Setting | Value |
|---|---|
| JDBC URL | `jdbc:h2:file:./data/taskmanager` |
| Username | `sa` |
| Password | Leave blank |

Click **Connect** to inspect the database tables and stored records.

---

## API Testing

Postman can be used to test the application's REST endpoints.

The following are representative examples of project endpoints:

| HTTP Method | Endpoint | Description |
|---|---|---|
| POST | `/api/projects` | Create a project |
| GET | `/api/projects` | Retrieve all projects |
| GET | `/api/projects/{id}` | Retrieve a project by ID |
| PUT | `/api/projects/{id}` | Update an existing project |

The exact endpoints and request fields depend on the implemented controllers and DTOs.

### Example: Create a Project

Send a POST request to:

```text
http://localhost:8080/api/projects
```

Select **Body → raw → JSON** in Postman and provide the project details expected by the backend.

Example request body:

```json
{
  "name": "Website Development",
  "description": "Development of a project management website"
}
```

Send the request and inspect the HTTP status code and JSON response.

**Note:** The example JSON is illustrative. Use the fields required by the project's implemented request model.

---

## Testing

The following scenarios are used to evaluate the application's main functionality:

1. **Project Creation:** Verify that a project can be created using valid details.
2. **Project Retrieval:** Verify that existing project records are returned.
3. **Project Updating:** Verify that project details can be modified.
4. **Task Creation:** Verify that a task can be created and associated with a project.
5. **Task Retrieval:** Verify that task details can be retrieved.
6. **Invalid Input:** Verify that invalid data produces an appropriate error response.
7. **Database Persistence:** Verify that saved project and task records remain available after restarting the backend.

Postman is used to inspect the HTTP status codes and response bodies. Database persistence is checked by restarting the application and retrieving previously saved records.

---

## Future Enhancements

Potential improvements include:

- Authentication and role-based access control.
- Notifications for approaching deadlines.
- Project progress dashboards and analytics.
- Task comments and file attachments.
- Automated unit and integration tests.
- Deployment to a cloud platform.

---

## Team Members

| Team Member | Contribution |
|---|---|
| Sachi | Project management module |
| Gargi | Task management module |
| Palak | Backend services and API functionality |
| Drushti | Frontend integration and testing |

---

## Repository

**GitHub:** https://github.com/gargi-327/Task-Management

---

## License

This project was developed as an academic mini-project for educational purposes.
