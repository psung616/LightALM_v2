package com.lightalm.customfield.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lightalm.customfield.domain.CustomFieldDefinition;
import com.lightalm.customfield.domain.CustomFieldStatus;
import com.lightalm.customfield.domain.CustomFieldValue;
import com.lightalm.customfield.dto.BulkSaveCustomFieldValuesRequest;
import com.lightalm.customfield.dto.CustomFieldValueItem;
import com.lightalm.customfield.dto.CustomFieldValueResponse;
import com.lightalm.customfield.repository.CustomFieldDefinitionRepository;
import com.lightalm.customfield.repository.CustomFieldValueRepository;
import com.lightalm.domain.ProjectRole;
import com.lightalm.domain.TargetType;
import com.lightalm.domain.User;
import com.lightalm.exception.ResourceNotFoundException;
import com.lightalm.exception.ValidationException;
import com.lightalm.repository.UserRepository;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.ProjectMemberService;
import com.lightalm.service.support.PolymorphicTargetValidator;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ADR-012 §A. 커스텀 필드 값 조회/일괄저장. 대상(target_type/target_id) 존재·프로젝트 소속
 * 검증은 {@link PolymorphicTargetValidator}를 재사용한다(ADR-010).
 */
@Service
@RequiredArgsConstructor
public class CustomFieldValueService {

    private final CustomFieldValueRepository customFieldValueRepository;
    private final CustomFieldDefinitionRepository customFieldDefinitionRepository;
    private final ProjectMemberService projectMemberService;
    private final PolymorphicTargetValidator polymorphicTargetValidator;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * 대상의 커스텀 필드 값 전체를 조회한다. ACTIVE 필드는 값이 없어도(=폼 초기값용) 항상 포함하고,
     * DEPRECATED 필드는 이 대상에 기존 값이 남아있는 경우에만 포함한다(ADR-012 §A.3 각주).
     */
    @Transactional(readOnly = true)
    public List<CustomFieldValueResponse> listValues(Long projectId, TargetType targetType, Long targetId, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.VIEWER);
        polymorphicTargetValidator.ensureExists(projectId, targetType, targetId);
        return buildResponse(projectId, targetType, targetId);
    }

    @Transactional
    public List<CustomFieldValueResponse> saveValues(Long projectId, TargetType targetType, Long targetId,
                                                       BulkSaveCustomFieldValuesRequest request, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.MEMBER);
        polymorphicTargetValidator.ensureExists(projectId, targetType, targetId);

        User updatedBy = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + principal.getId()));

        for (CustomFieldValueItem item : request.values()) {
            CustomFieldDefinition definition = customFieldDefinitionRepository.findByIdAndProjectId(item.fieldId(), projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("커스텀 필드를 찾을 수 없습니다: " + item.fieldId()));
            if (definition.getTargetType() != targetType) {
                throw new ValidationException("필드의 targetType이 요청과 일치하지 않습니다: " + item.fieldId());
            }

            Optional<CustomFieldValue> existing = customFieldValueRepository
                    .findByField_IdAndTargetTypeAndTargetId(definition.getId(), targetType, targetId);

            // ADR-012 §A.3: 비활성(DEPRECATED) 필드는 "새 입력 옵션으로 폼에 내놓지 않는다"는 뜻이지
            // 기존 값의 수정 자체를 막는다는 뜻이 아니다(qa-tester 반려 사유). 그래서 신규 생성만 막고,
            // 이미 값이 존재하는 경우의 수정은 필드 상태와 무관하게 허용한다.
            if (existing.isEmpty() && !definition.isActive()) {
                throw new ValidationException("비활성화된 필드에는 새 값을 생성할 수 없습니다: " + definition.getFieldKey());
            }

            validateValue(definition, item.value());
            upsert(definition, targetType, targetId, item.value(), updatedBy, existing);
        }

        return buildResponse(projectId, targetType, targetId);
    }

    private void upsert(CustomFieldDefinition definition, TargetType targetType, Long targetId, String rawValue, User updatedBy,
                         Optional<CustomFieldValue> existing) {
        existing.ifPresentOrElse(
                existingValue -> existingValue.changeValue(rawValue, updatedBy),
                () -> customFieldValueRepository.save(CustomFieldValue.builder()
                        .field(definition)
                        .targetType(targetType)
                        .targetId(targetId)
                        .value(rawValue)
                        .updatedBy(updatedBy)
                        .build()));
    }

    private void validateValue(CustomFieldDefinition definition, String value) {
        boolean blank = value == null || value.isBlank();
        if (Boolean.TRUE.equals(definition.getRequired()) && blank) {
            throw new ValidationException(definition.getLabel() + "은(는) 필수 입력 항목입니다.");
        }
        if (blank) {
            return;
        }
        switch (definition.getDataType()) {
            case NUMBER -> {
                try {
                    Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    throw new ValidationException(definition.getLabel() + " 값은 숫자여야 합니다.");
                }
            }
            case DATE -> {
                try {
                    LocalDate.parse(value);
                } catch (DateTimeParseException e) {
                    throw new ValidationException(definition.getLabel() + " 값은 ISO 날짜(yyyy-MM-dd) 형식이어야 합니다.");
                }
            }
            case BOOLEAN -> {
                if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                    throw new ValidationException(definition.getLabel() + " 값은 true/false여야 합니다.");
                }
            }
            case MULTI_SELECT -> {
                JsonNode node;
                try {
                    node = objectMapper.readTree(value);
                } catch (JsonProcessingException e) {
                    throw new ValidationException(definition.getLabel() + " 값은 JSON 배열 문자열이어야 합니다.");
                }
                if (!node.isArray()) {
                    throw new ValidationException(definition.getLabel() + " 값은 JSON 배열 문자열이어야 합니다.");
                }
            }
            case TEXT, SINGLE_SELECT -> {
                // 추가 형식 검증 없음
            }
        }
    }

    private List<CustomFieldValueResponse> buildResponse(Long projectId, TargetType targetType, Long targetId) {
        Map<Long, String> valueByFieldId = customFieldValueRepository
                .findByField_Project_IdAndTargetTypeAndTargetId(projectId, targetType, targetId)
                .stream()
                .collect(Collectors.toMap(v -> v.getField().getId(), CustomFieldValue::getValue));

        List<CustomFieldDefinition> activeDefinitions = customFieldDefinitionRepository
                .findByProjectIdAndTargetTypeAndStatusOrderByDisplayOrderAsc(projectId, targetType, CustomFieldStatus.ACTIVE);
        List<CustomFieldDefinition> deprecatedWithValue = customFieldDefinitionRepository
                .findByProjectIdAndTargetTypeAndStatusOrderByDisplayOrderAsc(projectId, targetType, CustomFieldStatus.DEPRECATED)
                .stream()
                .filter(definition -> valueByFieldId.containsKey(definition.getId()))
                .toList();

        return Stream.concat(activeDefinitions.stream(), deprecatedWithValue.stream())
                .sorted(Comparator.comparing(CustomFieldDefinition::getDisplayOrder))
                .map(definition -> CustomFieldValueResponse.of(definition, valueByFieldId.get(definition.getId())))
                .toList();
    }
}
