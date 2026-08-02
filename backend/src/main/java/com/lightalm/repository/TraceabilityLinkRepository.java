package com.lightalm.repository;

import com.lightalm.domain.TargetType;
import com.lightalm.domain.TraceabilityLink;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TraceabilityLinkRepository extends JpaRepository<TraceabilityLink, Long> {

    List<TraceabilityLink> findByProjectId(Long projectId);

    List<TraceabilityLink> findBySourceTypeAndSourceIdIn(TargetType sourceType, List<Long> sourceIds);

    List<TraceabilityLink> findBySourceTypeAndSourceId(TargetType sourceType, Long sourceId);

    List<TraceabilityLink> findByTargetTypeAndTargetId(TargetType targetType, Long targetId);

    boolean existsBySourceTypeAndSourceIdAndTargetTypeAndTargetIdAndLinkType(
            TargetType sourceType, Long sourceId, TargetType targetType, Long targetId, com.lightalm.domain.LinkType linkType);
}
