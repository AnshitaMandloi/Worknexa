package com.worknexa.rule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessRuleRepository extends JpaRepository<BusinessRule, Long> {

    List<BusinessRule> findAllByTenantIdAndWorkflowIdOrderByPriorityAsc(Long tenantId, Long workflowId);

    List<BusinessRule> findAllByTenantIdAndWorkflowIdAndActiveTrueOrderByPriorityAsc(Long tenantId, Long workflowId);

    Optional<BusinessRule> findByIdAndTenantId(Long id, Long tenantId);
}
