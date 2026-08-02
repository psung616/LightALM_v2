package com.lightalm.repository;

import com.lightalm.domain.Issue;
import com.lightalm.domain.IssueStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface IssueRepository extends JpaRepository<Issue, Long>, JpaSpecificationExecutor<Issue> {

    Optional<Issue> findByIssueKey(String issueKey);

    List<Issue> findByProjectId(Long projectId);

    long countByProjectIdAndStatus(Long projectId, IssueStatus status);

    long countByProjectIdAndAssigneeId(Long projectId, Long assigneeId);
}
