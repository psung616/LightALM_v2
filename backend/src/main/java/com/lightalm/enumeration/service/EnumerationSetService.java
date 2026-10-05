package com.lightalm.enumeration.service;

import com.lightalm.domain.Priority;
import com.lightalm.domain.Project;
import com.lightalm.domain.ProjectRole;
import com.lightalm.enumeration.domain.BaseEnumType;
import com.lightalm.enumeration.domain.EnumerationValueStatus;
import com.lightalm.enumeration.domain.ProjectEnumerationSet;
import com.lightalm.enumeration.domain.ProjectEnumerationValue;
import com.lightalm.enumeration.dto.CreateEnumerationSetRequest;
import com.lightalm.enumeration.dto.CreateEnumerationValueRequest;
import com.lightalm.enumeration.dto.EnumerationSetResponse;
import com.lightalm.enumeration.dto.EnumerationValueResponse;
import com.lightalm.enumeration.dto.UpdateEnumerationValueRequest;
import com.lightalm.enumeration.repository.ProjectEnumerationSetRepository;
import com.lightalm.enumeration.repository.ProjectEnumerationValueRepository;
import com.lightalm.exception.ResourceNotFoundException;
import com.lightalm.exception.ValidationException;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.ProjectMemberService;
import com.lightalm.service.ProjectService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ADR-012 §C. 프로젝트별 열거형 집합/값 CRUD — 쓰기는 모두 PROJECT_ADMIN+(§C.4).
 *
 * <p>§C.1의 핵심 제약: {@code base_enum}이 REQUIREMENT_STATUS/ISSUE_STATUS/TEST_CASE_STATUS인
 * 집합의 생성은 이 서비스가 명시적으로 거부한다. PRIORITY 또는 baseEnum 생략(커스텀 필드 전용)만
 * 허용한다 — 상태값(Status) 자체의 확장은 이번 범위 밖이다.</p>
 */
@Service
@RequiredArgsConstructor
public class EnumerationSetService {

    private static final String PRIORITY_ENUM_KEY = "PRIORITY";

    private static final Set<BaseEnumType> FORBIDDEN_BASE_ENUMS = Set.of(
            BaseEnumType.REQUIREMENT_STATUS, BaseEnumType.ISSUE_STATUS, BaseEnumType.TEST_CASE_STATUS);

    private final ProjectEnumerationSetRepository setRepository;
    private final ProjectEnumerationValueRepository valueRepository;
    private final ProjectService projectService;
    private final ProjectMemberService projectMemberService;

    @Transactional(readOnly = true)
    public List<EnumerationSetResponse> list(Long projectId, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.PROJECT_ADMIN);
        return setRepository.findByProjectId(projectId).stream()
                .map(set -> EnumerationSetResponse.from(set,
                        valueRepository.findByEnumerationSetIdOrderByDisplayOrderAsc(set.getId())))
                .toList();
    }

    @Transactional
    public EnumerationSetResponse create(Long projectId, CreateEnumerationSetRequest request, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.PROJECT_ADMIN);

        BaseEnumType baseEnum = request.baseEnum();
        if (baseEnum != null && FORBIDDEN_BASE_ENUMS.contains(baseEnum)) {
            throw new ValidationException(
                    "상태값(" + baseEnum + ") 자체의 프로젝트별 확장은 지원하지 않습니다. "
                            + "baseEnum은 PRIORITY 또는 생략(커스텀 필드 전용)만 허용됩니다.");
        }
        // qa-tester 반려(2026-10-05): validator는 enumKey="PRIORITY"로 집합을 찾고, 기본값 시드는 baseEnum=PRIORITY일
        // 때만 일어난다. 둘이 어긋나면(예: enumKey=PRIORITY + baseEnum 생략) 값 0개짜리 집합이 PRIORITY 검증을
        // 가로채 그 프로젝트의 모든 항목 생성/수정이 400이 되고, 집합 삭제 API가 없어 복구할 수 없었다.
        // 두 판별 기준이 항상 같은 집합을 가리키도록 "enumKey=PRIORITY ⇔ baseEnum=PRIORITY"를 생성 시점에 강제한다.
        boolean priorityKey = PRIORITY_ENUM_KEY.equals(request.enumKey());
        boolean priorityBase = baseEnum == BaseEnumType.PRIORITY;
        if (priorityKey != priorityBase) {
            throw new ValidationException(
                    "PRIORITY 확장 집합은 enumKey와 baseEnum을 모두 PRIORITY로 지정해야 합니다"
                            + "(커스텀 필드 전용 집합은 PRIORITY가 아닌 enumKey + baseEnum 생략).");
        }
        if (setRepository.existsByProjectIdAndEnumKey(projectId, request.enumKey())) {
            throw new ValidationException("이미 같은 enumKey의 열거형 집합이 존재합니다: " + request.enumKey());
        }

        Project project = projectService.getEntity(projectId);
        ProjectEnumerationSet set = ProjectEnumerationSet.builder()
                .project(project)
                .enumKey(request.enumKey())
                .baseEnum(baseEnum)
                .name(request.name())
                .build();
        ProjectEnumerationSet saved = setRepository.save(set);

        List<ProjectEnumerationValue> seeded = baseEnum == BaseEnumType.PRIORITY
                ? seedPriorityDefaults(saved)
                : List.of();
        return EnumerationSetResponse.from(saved, seeded);
    }

    /** 최초로 PRIORITY 집합을 만드는 순간, 기존 Java Priority enum 4개 값을 is_system_default=true로 시드한다(§C.3). */
    private List<ProjectEnumerationValue> seedPriorityDefaults(ProjectEnumerationSet set) {
        List<ProjectEnumerationValue> saved = new ArrayList<>();
        int order = 0;
        for (Priority priority : Priority.values()) {
            ProjectEnumerationValue value = ProjectEnumerationValue.builder()
                    .enumerationSet(set)
                    .valueKey(priority.name())
                    .label(priority.name())
                    .displayOrder(order++)
                    .isSystemDefault(true)
                    .build();
            saved.add(valueRepository.save(value));
        }
        return saved;
    }

    @Transactional
    public EnumerationValueResponse addValue(Long projectId, Long setId, CreateEnumerationValueRequest request,
                                              UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.PROJECT_ADMIN);
        ProjectEnumerationSet set = getScopedSet(projectId, setId);
        if (valueRepository.existsByEnumerationSetIdAndValueKey(setId, request.valueKey())) {
            throw new ValidationException("이미 같은 valueKey의 값이 존재합니다: " + request.valueKey());
        }

        int nextOrder = request.displayOrder() != null
                ? request.displayOrder()
                : valueRepository.findByEnumerationSetIdOrderByDisplayOrderAsc(setId).stream()
                        .mapToInt(ProjectEnumerationValue::getDisplayOrder)
                        .max()
                        .orElse(-1) + 1;

        ProjectEnumerationValue value = ProjectEnumerationValue.builder()
                .enumerationSet(set)
                .valueKey(request.valueKey())
                .label(request.label())
                .displayOrder(nextOrder)
                .isSystemDefault(false)
                .build();
        return EnumerationValueResponse.from(valueRepository.save(value));
    }

    @Transactional
    public EnumerationValueResponse updateValue(Long projectId, Long setId, Long valueId,
                                                 UpdateEnumerationValueRequest request, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.PROJECT_ADMIN);
        getScopedSet(projectId, setId);
        ProjectEnumerationValue value = getScopedValue(setId, valueId);
        value.updateLabelAndOrder(request.label(), request.displayOrder());
        return EnumerationValueResponse.from(value);
    }

    /** is_system_default=true인 값은 삭제(소프트)할 수 없다(§C.2) — 라벨/순서 수정은 updateValue로 계속 가능. */
    @Transactional
    public void deprecateValue(Long projectId, Long setId, Long valueId, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.PROJECT_ADMIN);
        getScopedSet(projectId, setId);
        ProjectEnumerationValue value = getScopedValue(setId, valueId);
        if (Boolean.TRUE.equals(value.getIsSystemDefault())) {
            throw new ValidationException("시스템 기본값은 삭제할 수 없습니다: " + value.getValueKey());
        }
        value.deprecate();
    }

    /**
     * 폼 렌더링용 활성 값 목록(VIEWER+). 프로젝트가 해당 enumKey 집합을 만들지 않았으면,
     * PRIORITY에 대해서는 기존 Java Priority enum 값을 그대로 돌려준다(§C.3 하위 호환).
     * 그 외 enumKey는 빈 목록을 돌려준다(커스텀 필드 전용 집합을 아직 만들지 않은 경우).
     */
    @Transactional(readOnly = true)
    public List<EnumerationValueResponse> listActiveValues(Long projectId, String enumKey, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.VIEWER);
        Optional<ProjectEnumerationSet> set = setRepository.findByProjectIdAndEnumKey(projectId, enumKey);
        if (set.isEmpty()) {
            return defaultActiveValues(enumKey);
        }
        return valueRepository
                .findByEnumerationSetIdAndStatusOrderByDisplayOrderAsc(set.get().getId(), EnumerationValueStatus.ACTIVE)
                .stream()
                .map(EnumerationValueResponse::from)
                .toList();
    }

    private List<EnumerationValueResponse> defaultActiveValues(String enumKey) {
        if (!"PRIORITY".equals(enumKey)) {
            return List.of();
        }
        List<EnumerationValueResponse> defaults = new ArrayList<>();
        int order = 0;
        for (Priority priority : Priority.values()) {
            defaults.add(new EnumerationValueResponse(
                    null, priority.name(), priority.name(), order++, true, EnumerationValueStatus.ACTIVE));
        }
        return defaults;
    }

    private ProjectEnumerationSet getScopedSet(Long projectId, Long setId) {
        return setRepository.findByIdAndProjectId(setId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("열거형 집합을 찾을 수 없습니다: " + setId));
    }

    private ProjectEnumerationValue getScopedValue(Long setId, Long valueId) {
        return valueRepository.findByIdAndEnumerationSetId(valueId, setId)
                .orElseThrow(() -> new ResourceNotFoundException("열거형 값을 찾을 수 없습니다: " + valueId));
    }
}
