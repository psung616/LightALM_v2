package com.lightalm.enumeration.repository;

import com.lightalm.enumeration.domain.ProjectEnumerationSet;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectEnumerationSetRepository extends JpaRepository<ProjectEnumerationSet, Long> {

    List<ProjectEnumerationSet> findByProjectId(Long projectId);

    Optional<ProjectEnumerationSet> findByProjectIdAndEnumKey(Long projectId, String enumKey);

    Optional<ProjectEnumerationSet> findByIdAndProjectId(Long id, Long projectId);

    boolean existsByProjectIdAndEnumKey(Long projectId, String enumKey);

    boolean existsByIdAndProjectId(Long id, Long projectId);
}
