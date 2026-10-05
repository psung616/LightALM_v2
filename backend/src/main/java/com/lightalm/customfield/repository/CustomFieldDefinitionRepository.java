package com.lightalm.customfield.repository;

import com.lightalm.customfield.domain.CustomFieldDefinition;
import com.lightalm.customfield.domain.CustomFieldStatus;
import com.lightalm.domain.TargetType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomFieldDefinitionRepository extends JpaRepository<CustomFieldDefinition, Long> {

    List<CustomFieldDefinition> findByProjectIdAndTargetTypeOrderByDisplayOrderAsc(Long projectId, TargetType targetType);

    List<CustomFieldDefinition> findByProjectIdAndTargetTypeAndStatusOrderByDisplayOrderAsc(
            Long projectId, TargetType targetType, CustomFieldStatus status);

    Optional<CustomFieldDefinition> findByIdAndProjectId(Long id, Long projectId);

    boolean existsByProjectIdAndTargetTypeAndFieldKey(Long projectId, TargetType targetType, String fieldKey);
}
