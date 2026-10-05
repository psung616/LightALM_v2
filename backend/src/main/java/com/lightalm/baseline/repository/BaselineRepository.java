package com.lightalm.baseline.repository;

import com.lightalm.baseline.domain.Baseline;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BaselineRepository extends JpaRepository<Baseline, Long> {

    List<Baseline> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    Optional<Baseline> findByIdAndProjectId(Long id, Long projectId);
}
