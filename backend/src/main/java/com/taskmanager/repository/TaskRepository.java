package com.taskmanager.repository;

import com.taskmanager.entity.Task;
import com.taskmanager.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;

/**
 * Summary queries owned by the project module. Member 2 can add task lifecycle
 * and filtering queries here without changing the project API contract.
 */
public interface TaskRepository extends JpaRepository<Task, Long> {
    long countByProjectId(Long projectId);
    long countByProjectIdAndStatus(Long projectId, TaskStatus status);

    @Query("""
            select count(t) from Task t
            where t.project.id = :projectId
              and t.dueDate < :today
              and t.status <> com.taskmanager.entity.TaskStatus.DONE
            """)
    long countOverdueByProjectId(@Param("projectId") Long projectId, @Param("today") LocalDate today);
}
