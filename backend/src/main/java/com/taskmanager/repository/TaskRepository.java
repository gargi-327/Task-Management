package com.taskmanager.repository;

import com.taskmanager.entity.Task;
import com.taskmanager.entity.TaskStatus;
import com.taskmanager.entity.TaskPriority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByProject_Id(Long projectId);

    List<Task> findByStatus(TaskStatus status);

    List<Task> findByPriority(TaskPriority priority);

    List<Task> findByAssignee_Id(Long assigneeId);

    List<Task> findByDueDate(LocalDate dueDate);

    List<Task> findByDueDateBeforeAndStatusNot(
            LocalDate date, TaskStatus status
    );

    // Count all tasks belonging to a project
    long countByProjectId(Long projectId);

    long countByProjectIdAndStatus(Long projectId, TaskStatus status);
    // Count overdue tasks that are not completed
    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.id = :projectId " +
            "AND t.dueDate < :today " +
            "AND t.status <> com.taskmanager.entity.TaskStatus.DONE")
    long countOverdueByProjectId(
            @Param("projectId") Long projectId,
            @Param("today") LocalDate today
    );
}