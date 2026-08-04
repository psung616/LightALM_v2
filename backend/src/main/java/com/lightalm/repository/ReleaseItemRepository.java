package com.lightalm.repository;

import com.lightalm.domain.ReleaseItem;
import com.lightalm.domain.TargetType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReleaseItemRepository extends JpaRepository<ReleaseItem, Long> {

    List<ReleaseItem> findByReleaseId(Long releaseId);

    Optional<ReleaseItem> findByIdAndReleaseId(Long id, Long releaseId);

    boolean existsByReleaseIdAndTargetTypeAndTargetId(Long releaseId, TargetType targetType, Long targetId);
}
