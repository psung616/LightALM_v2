package com.lightalm.customfield.repository;

import com.lightalm.customfield.domain.CustomFieldValue;
import com.lightalm.domain.TargetType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomFieldValueRepository extends JpaRepository<CustomFieldValue, Long> {

    List<CustomFieldValue> findByField_Project_IdAndTargetTypeAndTargetId(
            Long projectId, TargetType targetType, Long targetId);

    Optional<CustomFieldValue> findByField_IdAndTargetTypeAndTargetId(Long fieldId, TargetType targetType, Long targetId);
}
