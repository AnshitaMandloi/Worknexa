package com.worknexa.workflow;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowStageRepository extends JpaRepository<WorkflowStage, Long> {

    List<WorkflowStage> findAllByWorkflowIdOrderByOrderIndexAsc(Long workflowId);

    void deleteAllByWorkflowId(Long workflowId);

    Optional<WorkflowStage> findFirstByWorkflowIdOrderByOrderIndexAsc(Long workflowId);

    Optional<WorkflowStage> findFirstByWorkflowIdAndOrderIndexGreaterThanOrderByOrderIndexAsc(Long workflowId, Integer currentOrderIndex);
}
