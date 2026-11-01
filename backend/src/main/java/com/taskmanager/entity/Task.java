package com.taskmanager.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "tasks", indexes = {
        @Index(name = "idx_tasks_project_status", columnList = "project_id,status"),
        @Index(name = "idx_tasks_assignee_status", columnList = "assignee_id,status"),
        @Index(name = "idx_tasks_due_date", columnList = "due_date"),
        @Index(name = "idx_tasks_priority", columnList = "priority")
})
public class Task {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false) private Project project;
    @Column(nullable = false, length = 200) private String title;
    @Column(length = 4000) private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private TaskPriority priority;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private TaskStatus status;
    @Column(name = "start_date", nullable = false) private LocalDate startDate;
    @Column(name = "due_date", nullable = false) private LocalDate dueDate;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "assignee_id") private User assignee;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by_id", nullable = false) private User createdBy;
}
