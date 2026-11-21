package com.worknexa.request;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface WorkflowRequestRepository extends JpaRepository<WorkflowRequest, Long> {

    Page<WorkflowRequest> findAllByTenantId(Long tenantId, Pageable pageable);

    Page<WorkflowRequest> findAllByTenantIdAndRequesterId(Long tenantId, Long requesterId, Pageable pageable);

    Optional<WorkflowRequest> findByIdAndTenantId(Long id, Long tenantId);

    long countByTenantId(Long tenantId);

    long countByTenantIdAndStatus(Long tenantId, RequestStatus status);

    long countByTenantIdAndStatusIn(Long tenantId, Collection<RequestStatus> statuses);
}
