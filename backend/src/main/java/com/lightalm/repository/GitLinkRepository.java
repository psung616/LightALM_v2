package com.lightalm.repository;

import com.lightalm.domain.GitLink;
import com.lightalm.domain.TargetType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GitLinkRepository extends JpaRepository<GitLink, Long> {

    List<GitLink> findByTargetTypeAndTargetIdOrderByLinkedAtDesc(TargetType targetType, Long targetId);

    boolean existsByTargetTypeAndTargetIdAndCommitSha(TargetType targetType, Long targetId, String commitSha);

    boolean existsByTargetTypeAndTargetIdAndPrNumber(TargetType targetType, Long targetId, Integer prNumber);
}
