package com.lightalm.repository;

import com.lightalm.domain.JenkinsBuild;
import com.lightalm.domain.TargetType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JenkinsBuildRepository extends JpaRepository<JenkinsBuild, Long> {

    List<JenkinsBuild> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(TargetType targetType, Long targetId);

    Optional<JenkinsBuild> findByProjectIdAndJobNameAndBuildNumber(Long projectId, String jobName, Integer buildNumber);
}
