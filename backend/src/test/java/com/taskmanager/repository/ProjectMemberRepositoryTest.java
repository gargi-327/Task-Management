package com.taskmanager.repository;

import com.taskmanager.entity.Project;
import com.taskmanager.entity.ProjectMember;
import com.taskmanager.entity.Role;
import com.taskmanager.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ProjectMemberRepositoryTest {
    @Autowired private TestEntityManager entityManager;
    @Autowired private ProjectMemberRepository memberships;
@Autowired private ProjectRepository projects;

    @Test
    void findsMembershipsByProjectAndUserAndChecksExistence() {
        User user = new User();
        user.setName("Avery");
        user.setEmail("avery@example.com");
        user.setRole(Role.MEMBER);
        entityManager.persist(user);

        Project project = new Project();
        project.setName("Website");
        project.setCreatedBy(user);
        entityManager.persist(project);

        ProjectMember membership = new ProjectMember(project, user, "MEMBER");
        entityManager.persistAndFlush(membership);

        User manager = new User();
        manager.setName("Morgan");
        manager.setEmail("morgan@example.com");
        manager.setRole(Role.MANAGER);
        entityManager.persist(manager);
        entityManager.persistAndFlush(new ProjectMember(project, manager, "MANAGER"));

        assertEquals(2, memberships.findAllByProjectId(project.getId()).size());
        assertEquals(1, memberships.findAllByUserId(user.getId()).size());
        assertTrue(memberships.findByProjectIdAndUserId(project.getId(), user.getId()).isPresent());
        assertTrue(memberships.existsByProjectIdAndUserId(project.getId(), user.getId()));
        assertEquals(1, memberships.findAllByProjectIdAndMembershipRoleForUpdate(
                project.getId(), "MANAGER").size());
        assertEquals(project.getId(), projects.findByIdForUpdate(project.getId()).orElseThrow().getId());
        assertFalse(memberships.existsByProjectIdAndUserId(project.getId(), 999L));
    }
}
