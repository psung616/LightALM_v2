package com.lightalm.workflow.repository;

import com.lightalm.domain.TargetType;
import com.lightalm.workflow.domain.WorkflowTransitionRule;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowTransitionRuleRepository extends JpaRepository<WorkflowTransitionRule, Long> {

    List<WorkflowTransitionRule> findByProjectIdAndTargetType(Long projectId, TargetType targetType);

    boolean existsByProjectIdAndTargetTypeAndFromStatusAndToStatus(
            Long projectId, TargetType targetType, String fromStatus, String toStatus);

    Optional<WorkflowTransitionRule> findByIdAndProjectId(Long id, Long projectId);
}
