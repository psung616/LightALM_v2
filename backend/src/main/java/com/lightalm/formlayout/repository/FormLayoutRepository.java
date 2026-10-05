package com.lightalm.formlayout.repository;

import com.lightalm.domain.TargetType;
import com.lightalm.formlayout.domain.FormLayout;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormLayoutRepository extends JpaRepository<FormLayout, Long> {

    Optional<FormLayout> findByProjectIdAndTargetType(Long projectId, TargetType targetType);
}
