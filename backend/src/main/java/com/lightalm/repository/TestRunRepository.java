package com.lightalm.repository;

import com.lightalm.domain.TestRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TestRunRepository extends JpaRepository<TestRun, Long>, JpaSpecificationExecutor<TestRun> {
}
