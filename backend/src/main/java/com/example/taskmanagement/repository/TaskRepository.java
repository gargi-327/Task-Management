package com.example.taskmanagement.repository;

import com.example.taskmanagement.entity.Task;
import com.example.taskmanagement.enums.TaskStatus;
import com.example.taskmanagement.enums.Priority;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByDueDate(LocalDate dueDate);

    List<Task> findByProject_Id(Long projectId);

    List<Task> findByStatus(TaskStatus status);

    List<Task> findByPriority(Priority priority);

    List<Task> findByAssignee_Id(Long assigneeId);

    List<Task> findByDueDateBeforeAndStatusNot(
            LocalDate date, TaskStatus status);
}