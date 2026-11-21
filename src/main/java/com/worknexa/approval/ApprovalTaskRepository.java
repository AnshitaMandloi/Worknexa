package com.worknexa.approval;

import com.worknexa.user.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalTaskRepository extends JpaRepository<ApprovalTask, Long> {

    List<ApprovalTask> findAllByTenantIdAndRequestIdOrderBySequenceNoAsc(Long tenantId, Long requestId);

    List<ApprovalTask> findAllByTenantIdAndAssignedRoleAndStatusOrderByCreatedAtDesc(Long tenantId, Role assignedRole, TaskStatus status);

    @Query("SELECT t FROM ApprovalTask t WHERE t.tenantId = :tenantId AND t.assignedRole = :role AND t.status = 'PENDING' " +
           "AND NOT EXISTS (SELECT prev FROM ApprovalTask prev WHERE prev.requestId = t.requestId AND prev.sequenceNo < t.sequenceNo AND prev.status != com.worknexa.approval.TaskStatus.APPROVED) " +
           "ORDER BY t.createdAt DESC")
    List<ApprovalTask> findActionableTasksForRole(@Param("tenantId") Long tenantId, @Param("role") Role role);

    @Query("SELECT t FROM ApprovalTask t WHERE t.tenantId = :tenantId AND t.assignedRole = :role AND t.status = 'PENDING' " +
           "AND NOT EXISTS (SELECT prev FROM ApprovalTask prev WHERE prev.requestId = t.requestId AND prev.sequenceNo < t.sequenceNo AND prev.status != com.worknexa.approval.TaskStatus.APPROVED) " +
           "ORDER BY t.createdAt DESC")
    Page<ApprovalTask> findActionableTasksForRole(@Param("tenantId") Long tenantId, @Param("role") Role role, Pageable pageable);

    Optional<ApprovalTask> findByIdAndTenantId(Long id, Long tenantId);

    long countByTenantIdAndStatus(Long tenantId, TaskStatus status);
}
