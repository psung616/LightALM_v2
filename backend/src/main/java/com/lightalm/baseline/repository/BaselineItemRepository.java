package com.lightalm.baseline.repository;

import com.lightalm.baseline.domain.BaselineItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BaselineItemRepository extends JpaRepository<BaselineItem, Long> {

    List<BaselineItem> findByBaselineIdOrderById(Long baselineId);

    long countByBaselineId(Long baselineId);
}
