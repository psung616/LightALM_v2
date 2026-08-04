package com.lightalm.repository;

import com.lightalm.domain.TestRunResult;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestRunResultRepository extends JpaRepository<TestRunResult, Long> {

    List<TestRunResult> findByTestRunId(Long testRunId);

    Optional<TestRunResult> findByTestRunIdAndTestCaseId(Long testRunId, Long testCaseId);
}
