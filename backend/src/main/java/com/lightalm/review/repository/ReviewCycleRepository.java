package com.lightalm.review.repository;

import com.lightalm.domain.TargetType;
import com.lightalm.review.domain.ReviewCycle;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewCycleRepository extends JpaRepository<ReviewCycle, Long> {

    List<ReviewCycle> findByProjectIdAndTargetTypeAndTargetIdOrderByCreatedAtDesc(
            Long projectId, TargetType targetType, Long targetId);

    Optional<ReviewCycle> findByIdAndProjectId(Long id, Long projectId);
}
