package com.lightalm.repository;

import com.lightalm.domain.Requirement;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RequirementRepository extends JpaRepository<Requirement, Long>, JpaSpecificationExecutor<Requirement> {

    Optional<Requirement> findByReqKey(String reqKey);

    List<Requirement> findByParentRequirementId(Long parentRequirementId);

    List<Requirement> findByProjectId(Long projectId);

    List<Requirement> findTop10ByProjectIdOrderByUpdatedAtDesc(Long projectId);

    List<Requirement> findByProjectIdInAndAssignedToId(List<Long> projectIds, Long assignedToId);

    long countByProjectIdAndStatus(Long projectId, com.lightalm.domain.RequirementStatus status);
}
