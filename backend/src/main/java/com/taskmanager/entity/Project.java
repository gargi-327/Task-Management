package com.taskmanager.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "projects", indexes = {
        @Index(name = "idx_projects_archived", columnList = "archived"),
        @Index(name = "idx_projects_created_by", columnList = "created_by_id"),
        @Index(name = "idx_projects_dates", columnList = "start_date,due_date")
})
public class Project {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 150) private String name;
    @Column(length = 2000) private String description;
    @Column(name = "start_date") private LocalDate startDate;
    @Column(name = "due_date") private LocalDate dueDate;
    @Column(nullable = false) private boolean archived = false;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getDueDate() { return dueDate; }
    public boolean isArchived() { return archived; }
    public User getCreatedBy() { return createdBy; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public void setArchived(boolean archived) { this.archived = archived; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
}
