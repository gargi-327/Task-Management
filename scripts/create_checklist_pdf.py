from pathlib import Path
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import KeepTogether, PageBreak, Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle


OUTPUT = Path(r"C:\Projects\task_manager\docs\Task_Manager_Implementation_Checklist.pdf")


def p(text, style):
    return Paragraph(text, style)


def footer(canvas, doc):
    canvas.saveState()
    canvas.setStrokeColor(colors.HexColor("#CBD5E1"))
    canvas.line(18 * mm, 14 * mm, A4[0] - 18 * mm, 14 * mm)
    canvas.setFont("Helvetica", 8)
    canvas.setFillColor(colors.HexColor("#64748B"))
    canvas.drawString(18 * mm, 9 * mm, "Task Manager - implementation checklist")
    canvas.drawRightString(A4[0] - 18 * mm, 9 * mm, f"Page {doc.page}")
    canvas.restoreState()


def build():
    styles = getSampleStyleSheet()
    title = ParagraphStyle("Title", parent=styles["Title"], fontName="Helvetica-Bold", fontSize=24, leading=29,
                           alignment=TA_CENTER, textColor=colors.HexColor("#0F172A"), spaceAfter=8)
    subtitle = ParagraphStyle("Subtitle", parent=styles["Normal"], fontSize=11, leading=16, alignment=TA_CENTER,
                              textColor=colors.HexColor("#475569"), spaceAfter=16)
    heading = ParagraphStyle("Heading", parent=styles["Heading2"], fontName="Helvetica-Bold", fontSize=14, leading=18,
                             textColor=colors.HexColor("#0F766E"), spaceBefore=12, spaceAfter=6)
    body = ParagraphStyle("Body", parent=styles["BodyText"], fontSize=9.5, leading=13, textColor=colors.HexColor("#1E293B"))
    small = ParagraphStyle("Small", parent=body, fontSize=8.5, leading=11)

    doc = SimpleDocTemplate(str(OUTPUT), pagesize=A4, rightMargin=18 * mm, leftMargin=18 * mm,
                            topMargin=17 * mm, bottomMargin=20 * mm)
    story = [p("Task and Project Management Tool", title),
             p("Four-member implementation checklist - Java Spring Boot backend + separate HTML/CSS/JavaScript frontend", subtitle)]

    story += [p("Project outcome", heading),
              p("Deliver a backend application that lets managers organise projects and coordinate team work. Team members can view their permitted work and update the status of tasks assigned to them. The frontend is a separate, simple responsive layer that calls the REST API.", body), Spacer(1, 6)]

    plan_rows = [[p("Phase", small), p("Completion target", small)],
                 [p("1. Planning", small), p("Agree stack, database model, endpoint names, JSON fields, enums, date format, status transitions, roles, branches, and CORS/API base URL.", small)],
                 [p("2. Skeleton", small), p("Create Spring Boot project and database connection; establish common errors; create frontend pages, CSS, and API helper.", small)],
                 [p("3. Build", small), p("Implement project, task, user/membership, permission, test, and frontend modules in parallel.", small)],
                 [p("4. Integrate", small), p("Verify create project -> create/assign task -> filter -> update status -> overdue and summary flows. Resolve CORS and contract mismatches.", small)],
                 [p("5. Submit", small), p("Run tests; prepare ER diagram/schema, Postman cases, README, screenshots/demo, GitHub source, and viva demonstration.", small)]]
    table = Table(plan_rows, colWidths=[36 * mm, 136 * mm], repeatRows=1)
    table.setStyle(TableStyle([("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#0F766E")), ("TEXTCOLOR", (0, 0), (-1, 0), colors.white), ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"), ("VALIGN", (0, 0), (-1, -1), "TOP"), ("GRID", (0, 0), (-1, -1), 0.35, colors.HexColor("#CBD5E1")), ("BACKGROUND", (0, 1), (-1, -1), colors.HexColor("#F8FAFC")), ("LEFTPADDING", (0, 0), (-1, -1), 6), ("RIGHTPADDING", (0, 0), (-1, -1), 6), ("TOPPADDING", (0, 0), (-1, -1), 6), ("BOTTOMPADDING", (0, 0), (-1, -1), 6)]))
    story += [table, Spacer(1, 8)]

    work = [
        ("Member 1 - Project and database", ["Design ER diagram/schema: User, Project, ProjectMember, Task; define keys, relationships, indexes.", "Build Project entity, repository, service, controller; create, list, detail, update, archive endpoints.", "Use an archived flag; add project summary with total, TODO, in-progress, done, and overdue counts.", "Validate required project fields and test create, view, update, archive, and unknown IDs."]),
        ("Member 2 - Task management and business rules", ["Build Task entity, repository, service, controller; enforce relationships to Project and User.", "Create/list/update tasks; support filters: project, status, priority, assignee, due date.", "Reject missing required fields, invalid project IDs, and due dates before start dates.", "Enforce transitions: TODO -> IN_PROGRESS -> DONE; decide whether IN_PROGRESS -> TODO is allowed. Detect overdue tasks when due date is past and status is not DONE."]),
        ("Member 3 - Users, permissions, and API testing", ["Build User and ProjectMember functions; prevent duplicate membership.", "Managers/admins create, update, archive projects and manage assignment; members only see permitted projects and update their assigned task status.", "Add service-layer permission checks. Add JWT/role-based authentication only after core APIs are stable.", "Create Postman positive/negative cases and integration tests for fields, dates, IDs, filters, permissions, and status transitions."]),
        ("Member 4 - Frontend and end-to-end integration", ["Create independent pages: dashboard, projects, project details, tasks; add shared CSS and JavaScript.", "Build navigation, project list/forms, task table/form, status controls, filters, messages, and responsive layout.", "Use fetch() to call the API. Send ISO dates and agreed enum values; backend validation remains authoritative.", "Run complete end-to-end workflow and capture screenshots/demo evidence."]),
    ]
    for index, (member, items) in enumerate(work):
        if index == 2:
            story.append(PageBreak())
        story.append(p(member, heading))
        for item in items:
            story.append(p("[ ] " + item, body))
            story.append(Spacer(1, 3))

    story += [PageBreak(), p("Shared API and validation checklist", heading)]
    api_rows = [[p("Endpoint", small), p("Check", small)],
                [p("POST /api/projects", small), p("Manager-only creation; required fields and error response.", small)],
                [p("GET /api/projects and /api/projects/{id}", small), p("Visibility rules; details; archived project semantics.", small)],
                [p("PUT /api/projects/{id} and PATCH /api/projects/{id}/archive", small), p("Manager permission; valid ID; archive flag rather than hard delete.", small)],
                [p("POST /api/projects/{id}/tasks", small), p("Valid project; required task fields; assignee/membership decision.", small)],
                [p("GET /api/tasks?projectId&status&priority&assigneeId&dueDate", small), p("Every filter works alone and together; sorting/pagination optional.", small)],
                [p("PATCH /api/tasks/{id}/status", small), p("Assigned-user/manager permission and only approved state changes.", small)],
                [p("GET /api/projects/{id}/summary", small), p("Accurate total, TODO, in-progress, done, overdue counts.", small)]]
    api = Table(api_rows, colWidths=[71 * mm, 101 * mm], repeatRows=1)
    api.setStyle(TableStyle([("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#0F766E")), ("TEXTCOLOR", (0, 0), (-1, 0), colors.white), ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"), ("VALIGN", (0, 0), (-1, -1), "TOP"), ("GRID", (0, 0), (-1, -1), 0.35, colors.HexColor("#CBD5E1")), ("BACKGROUND", (0, 1), (-1, -1), colors.HexColor("#F8FAFC")), ("LEFTPADDING", (0, 0), (-1, -1), 6), ("RIGHTPADDING", (0, 0), (-1, -1), 6), ("TOPPADDING", (0, 0), (-1, -1), 6), ("BOTTOMPADDING", (0, 0), (-1, -1), 6)]))
    story += [api, p("Final delivery checklist", heading)]
    final_items = ["Working Spring Boot backend and integrated, separate HTML/CSS/JavaScript frontend.", "GitHub repository with regular commits and clear ownership/branches.", "ER diagram/database schema; API contract and assumptions.", "Postman collection with successful and failing requests.", "Unit and integration tests for core business rules.", "README with setup steps, database configuration, API base URL, and test/demo instructions.", "Demo flow and screenshots; optional extensions only after the core is complete."]
    for item in final_items:
        story.append(p("[ ] " + item, body))
        story.append(Spacer(1, 3))

    doc.build(story, onFirstPage=footer, onLaterPages=footer)


if __name__ == "__main__":
    build()
