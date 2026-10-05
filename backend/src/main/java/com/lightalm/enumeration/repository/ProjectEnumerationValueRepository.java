package com.lightalm.enumeration.repository;

import com.lightalm.enumeration.domain.EnumerationValueStatus;
import com.lightalm.enumeration.domain.ProjectEnumerationValue;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectEnumerationValueRepository extends JpaRepository<ProjectEnumerationValue, Long> {

    List<ProjectEnumerationValue> findByEnumerationSetIdOrderByDisplayOrderAsc(Long enumerationSetId);

    List<ProjectEnumerationValue> findByEnumerationSetIdAndStatusOrderByDisplayOrderAsc(
            Long enumerationSetId, EnumerationValueStatus status);

    Optional<ProjectEnumerationValue> findByIdAndEnumerationSetId(Long id, Long enumerationSetId);

    boolean existsByEnumerationSetIdAndValueKey(Long enumerationSetId, String valueKey);

    boolean existsByEnumerationSetIdAndValueKeyAndStatus(Long enumerationSetId, String valueKey, EnumerationValueStatus status);
}
