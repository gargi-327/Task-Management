package com.taskmanager.repository;

import com.taskmanager.entity.ProjectMember;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
    List<ProjectMember> findAllByProjectId(Long projectId);
    List<ProjectMember> findAllByUserId(Long userId);
    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);
    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select membership from ProjectMember membership "
            + "where membership.project.id = :projectId "
            + "and membership.membershipRole = :membershipRole")
    List<ProjectMember> findAllByProjectIdAndMembershipRoleForUpdate(
            @Param("projectId") Long projectId,
            @Param("membershipRole") String membershipRole);
}
