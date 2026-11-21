package com.worknexa.workflow;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, Long> {

    List<WorkflowDefinition> findAllByTenantId(Long tenantId);

    Page<WorkflowDefinition> findAllByTenantId(Long tenantId, Pageable pageable);

    Optional<WorkflowDefinition> findByIdAndTenantId(Long id, Long tenantId);

    Optional<WorkflowDefinition> findByTenantIdAndRequestTypeIgnoreCaseAndActiveTrue(Long tenantId, String requestType);

    boolean existsByTenantIdAndNameIgnoreCase(Long tenantId, String name);
}
