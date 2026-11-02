package com.taskmanager.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "project_members", uniqueConstraints = @UniqueConstraint(name = "uk_project_member", columnNames = {"project_id", "user_id"}), indexes = @Index(name = "idx_project_members_user", columnList = "user_id"))
public class ProjectMember {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false) private Project project;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(name = "membership_role", nullable = false, length = 30) private String membershipRole;

    protected ProjectMember() { }

    public ProjectMember(Project project, User user, String membershipRole) {
        this.project = project;
        this.user = user;
        this.membershipRole = membershipRole;
    }

    public Project getProject() { return project; }
    public User getUser() { return user; }
    public String getMembershipRole() { return membershipRole; }
}
