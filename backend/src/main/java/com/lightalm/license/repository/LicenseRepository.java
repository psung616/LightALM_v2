package com.lightalm.license.repository;

import com.lightalm.license.domain.License;
import com.lightalm.license.domain.LicenseStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LicenseRepository extends JpaRepository<License, Long> {

    Optional<License> findByStatus(LicenseStatus status);

    Page<License> findAllByOrderByUploadedAtDesc(Pageable pageable);
}
