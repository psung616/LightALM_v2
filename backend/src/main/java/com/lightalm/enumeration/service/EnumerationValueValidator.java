package com.lightalm.enumeration.service;

import com.lightalm.domain.Priority;
import com.lightalm.enumeration.domain.EnumerationValueStatus;
import com.lightalm.enumeration.domain.ProjectEnumerationSet;
import com.lightalm.enumeration.repository.ProjectEnumerationSetRepository;
import com.lightalm.enumeration.repository.ProjectEnumerationValueRepository;
import com.lightalm.exception.ValidationException;
import java.util.Arrays;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * ADR-012 §C.3. {@code priority} 값 검증의 유일한 진입점.
 *
 * <p>프로젝트가 해당 {@code enumKey}에 대해 {@code project_enumeration_sets}를 만들지 않았으면
 * 기존 고정 Java enum 값으로 검증한다(지금은 PRIORITY만 fallback을 정의한다) — 즉
 * <b>이 validator를 쓰지 않던 기존 동작은 전혀 바뀌지 않는다</b>. 집합을 만들었으면 그 집합의
 * ACTIVE 값 목록으로 검증한다.</p>
 *
 * <p>requirements/issues/test_cases의 {@code priority} 컬럼 DB CHECK 제약이 제거되었으므로
 * (V15 마이그레이션), 이 validator가 호출되지 않는 쓰기 경로가 생기면 DB 안전망 없이 잘못된
 * 값이 저장될 수 있다 — 반드시 모든 priority 쓰기 경로(생성/수정)에서 호출해야 한다.</p>
 */
@Component
@RequiredArgsConstructor
public class EnumerationValueValidator {

    private static final String PRIORITY_ENUM_KEY = "PRIORITY";

    private final ProjectEnumerationSetRepository setRepository;
    private final ProjectEnumerationValueRepository valueRepository;

    @Transactional(readOnly = true)
    public void requireValidValue(Long projectId, String enumKey, String value) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(enumKey + " 값은 필수입니다.");
        }

        Optional<ProjectEnumerationSet> set = setRepository.findByProjectIdAndEnumKey(projectId, enumKey);
        if (set.isEmpty()) {
            requireValidDefaultValue(enumKey, value);
            return;
        }

        boolean valid = valueRepository.existsByEnumerationSetIdAndValueKeyAndStatus(
                set.get().getId(), value, EnumerationValueStatus.ACTIVE);
        if (!valid) {
            throw new ValidationException("유효하지 않은 " + enumKey + " 값입니다: " + value);
        }
    }

    /**
     * 수정(update) 경로 전용. 값이 기존 값과 같으면(=사용자가 이 필드를 바꾸지 않았으면) 검증을 생략하고,
     * 다른 값으로 바꿀 때만 {@link #requireValidValue}로 ACTIVE 여부를 검사한다.
     *
     * <p>qa-tester 반려(2026-10-05): 이미 저장된 값이 나중에 DEPRECATED 처리되면, 그 항목은 priority를
     * 건드리지 않은 다른 필드 수정까지 400으로 막혔다. Phase 20 커스텀 필드의 DEPRECATED 처리와 같은 원칙
     * — "폐기 = 새로 고를 수 없음"이지 "기존 값 보유 항목 잠금"이 아니다 — 으로 맞춘다(ADR-012 §C 각주).</p>
     */
    @Transactional(readOnly = true)
    public void requireValidValueForChange(Long projectId, String enumKey, String currentValue, String newValue) {
        if (newValue != null && newValue.equals(currentValue)) {
            return;
        }
        requireValidValue(projectId, enumKey, newValue);
    }

    private void requireValidDefaultValue(String enumKey, String value) {
        if (!PRIORITY_ENUM_KEY.equals(enumKey)) {
            throw new ValidationException("정의되지 않은 열거형입니다: " + enumKey);
        }
        boolean valid = Arrays.stream(Priority.values()).anyMatch(priority -> priority.name().equals(value));
        if (!valid) {
            throw new ValidationException("유효하지 않은 PRIORITY 값입니다: " + value);
        }
    }
}
