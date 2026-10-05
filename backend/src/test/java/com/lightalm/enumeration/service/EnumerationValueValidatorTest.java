package com.lightalm.enumeration.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.lightalm.domain.Project;
import com.lightalm.enumeration.domain.BaseEnumType;
import com.lightalm.enumeration.domain.EnumerationValueStatus;
import com.lightalm.enumeration.domain.ProjectEnumerationSet;
import com.lightalm.enumeration.repository.ProjectEnumerationSetRepository;
import com.lightalm.enumeration.repository.ProjectEnumerationValueRepository;
import com.lightalm.exception.ValidationException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * ADR-012 §C.3. PRIORITY 검증의 유일한 진입점.
 * "집합을 만들지 않은 프로젝트는 기존 동작이 전혀 바뀌지 않는다"는 핵심 회귀 보장을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class EnumerationValueValidatorTest {

    @Mock
    private ProjectEnumerationSetRepository setRepository;
    @Mock
    private ProjectEnumerationValueRepository valueRepository;

    @InjectMocks
    private EnumerationValueValidator enumerationValueValidator;

    @Test
    void requireValidValue_whenNoSetExists_acceptsExistingJavaEnumValues() {
        when(setRepository.findByProjectIdAndEnumKey(10L, "PRIORITY")).thenReturn(Optional.empty());

        assertThatCode(() -> enumerationValueValidator.requireValidValue(10L, "PRIORITY", "LOW")).doesNotThrowAnyException();
        assertThatCode(() -> enumerationValueValidator.requireValidValue(10L, "PRIORITY", "MEDIUM")).doesNotThrowAnyException();
        assertThatCode(() -> enumerationValueValidator.requireValidValue(10L, "PRIORITY", "HIGH")).doesNotThrowAnyException();
        assertThatCode(() -> enumerationValueValidator.requireValidValue(10L, "PRIORITY", "CRITICAL")).doesNotThrowAnyException();
    }

    @Test
    void requireValidValue_whenNoSetExists_rejectsValueOutsideJavaEnum() {
        when(setRepository.findByProjectIdAndEnumKey(10L, "PRIORITY")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enumerationValueValidator.requireValidValue(10L, "PRIORITY", "BLOCKER"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void requireValidValue_whenSetExists_acceptsActiveCustomValue() {
        Project project = Project.builder().id(10L).projectKey("LALM").name("Light ALM").build();
        ProjectEnumerationSet set = ProjectEnumerationSet.builder()
                .project(project).enumKey("PRIORITY").baseEnum(BaseEnumType.PRIORITY).name("우선순위 확장").build();
        when(setRepository.findByProjectIdAndEnumKey(10L, "PRIORITY")).thenReturn(Optional.of(set));
        when(valueRepository.existsByEnumerationSetIdAndValueKeyAndStatus(null, "BLOCKER", EnumerationValueStatus.ACTIVE))
                .thenReturn(true);

        assertThatCode(() -> enumerationValueValidator.requireValidValue(10L, "PRIORITY", "BLOCKER"))
                .doesNotThrowAnyException();
    }

    @Test
    void requireValidValue_whenSetExists_rejectsValueNotInSet() {
        Project project = Project.builder().id(10L).projectKey("LALM").name("Light ALM").build();
        ProjectEnumerationSet set = ProjectEnumerationSet.builder()
                .project(project).enumKey("PRIORITY").baseEnum(BaseEnumType.PRIORITY).name("우선순위 확장").build();
        when(setRepository.findByProjectIdAndEnumKey(10L, "PRIORITY")).thenReturn(Optional.of(set));
        when(valueRepository.existsByEnumerationSetIdAndValueKeyAndStatus(null, "GHOST", EnumerationValueStatus.ACTIVE))
                .thenReturn(false);

        assertThatThrownBy(() -> enumerationValueValidator.requireValidValue(10L, "PRIORITY", "GHOST"))
                .isInstanceOf(ValidationException.class);
    }

    /** qa-tester 반려(2026-10-05): 기존 값이 DEPRECATED여도 값을 바꾸지 않는 수정은 검증을 생략한다. */
    @Test
    void requireValidValueForChange_whenValueUnchanged_skipsValidationEvenIfDeprecated() {
        assertThatCode(() -> enumerationValueValidator.requireValidValueForChange(10L, "PRIORITY", "BLOCKER", "BLOCKER"))
                .doesNotThrowAnyException();
        org.mockito.Mockito.verifyNoInteractions(setRepository, valueRepository);
    }

    @Test
    void requireValidValueForChange_whenChangingToDeprecatedValue_isRejected() {
        Project project = Project.builder().id(10L).projectKey("LALM").name("Light ALM").build();
        ProjectEnumerationSet set = ProjectEnumerationSet.builder()
                .project(project).enumKey("PRIORITY").baseEnum(BaseEnumType.PRIORITY).name("우선순위 확장").build();
        when(setRepository.findByProjectIdAndEnumKey(10L, "PRIORITY")).thenReturn(Optional.of(set));
        when(valueRepository.existsByEnumerationSetIdAndValueKeyAndStatus(null, "BLOCKER", EnumerationValueStatus.ACTIVE))
                .thenReturn(false);

        assertThatThrownBy(() -> enumerationValueValidator.requireValidValueForChange(10L, "PRIORITY", "LOW", "BLOCKER"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void requireValidValue_whenValueIsNull_throwsValidationException() {
        assertThatThrownBy(() -> enumerationValueValidator.requireValidValue(10L, "PRIORITY", null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void requireValidValue_whenNoSetExistsForUnknownEnumKey_throwsValidationException() {
        when(setRepository.findByProjectIdAndEnumKey(10L, "SEVERITY")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enumerationValueValidator.requireValidValue(10L, "SEVERITY", "HIGH"))
                .isInstanceOf(ValidationException.class);
    }
}
