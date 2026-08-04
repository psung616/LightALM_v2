package com.lightalm.repository;

import com.lightalm.domain.Release;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ReleaseRepository extends JpaRepository<Release, Long>, JpaSpecificationExecutor<Release> {

    boolean existsByProjectIdAndVersion(Long projectId, String version);
}
