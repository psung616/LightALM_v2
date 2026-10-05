package com.lightalm.formlayout.service;

import com.lightalm.customfield.domain.CustomFieldDefinition;
import com.lightalm.customfield.repository.CustomFieldDefinitionRepository;
import com.lightalm.domain.ProjectRole;
import com.lightalm.domain.TargetType;
import com.lightalm.exception.ResourceNotFoundException;
import com.lightalm.exception.ValidationException;
import com.lightalm.formlayout.domain.FieldSource;
import com.lightalm.formlayout.domain.FormLayout;
import com.lightalm.formlayout.domain.FormLayoutField;
import com.lightalm.formlayout.domain.FormLayoutSection;
import com.lightalm.formlayout.dto.FormLayoutFieldResponse;
import com.lightalm.formlayout.dto.FormLayoutResponse;
import com.lightalm.formlayout.dto.FormLayoutSectionResponse;
import com.lightalm.formlayout.dto.SaveFormLayoutFieldRequest;
import com.lightalm.formlayout.dto.SaveFormLayoutRequest;
import com.lightalm.formlayout.dto.SaveFormLayoutSectionRequest;
import com.lightalm.formlayout.repository.FormLayoutRepository;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.ProjectMemberService;
import com.lightalm.service.ProjectService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ADR-012 §B. 프로젝트+target_type당 폼 레이아웃(섹션+필드 트리) 조회/전체 치환.
 * 레이아웃이 설정되지 않은 프로젝트+target_type은 {@link StandardFieldKeyRegistry}의 기본
 * 순서로 합성한 "기본 레이아웃"을 반환한다(§B.3 하위호환 규칙).
 */
@Service
@RequiredArgsConstructor
public class FormLayoutService {

    private final FormLayoutRepository formLayoutRepository;
    private final CustomFieldDefinitionRepository customFieldDefinitionRepository;
    private final ProjectMemberService projectMemberService;
    private final ProjectService projectService;
    private final StandardFieldKeyRegistry standardFieldKeyRegistry;

    @Transactional(readOnly = true)
    public FormLayoutResponse getForConfig(Long projectId, TargetType targetType, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.PROJECT_ADMIN);
        return getOrDefault(projectId, targetType);
    }

    @Transactional(readOnly = true)
    public FormLayoutResponse getForRender(Long projectId, TargetType targetType, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.VIEWER);
        return getOrDefault(projectId, targetType);
    }

    @Transactional
    public FormLayoutResponse saveLayout(Long projectId, TargetType targetType, SaveFormLayoutRequest request,
                                          UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.PROJECT_ADMIN);

        Map<Long, CustomFieldDefinition> customFieldsById = validateAndCollectCustomFields(projectId, targetType, request);
        validateAtLeastOneVisibleStandardField(request);

        formLayoutRepository.findByProjectIdAndTargetType(projectId, targetType)
                .ifPresent(existing -> {
                    formLayoutRepository.delete(existing);
                    formLayoutRepository.flush();
                });

        FormLayout layout = FormLayout.builder()
                .project(projectService.getEntity(projectId))
                .targetType(targetType)
                .build();

        int sectionOrder = 0;
        for (SaveFormLayoutSectionRequest sectionRequest : request.sections()) {
            FormLayoutSection section = FormLayoutSection.builder()
                    .title(sectionRequest.title())
                    .displayOrder(sectionRequest.displayOrder() != null ? sectionRequest.displayOrder() : sectionOrder)
                    .build();
            int fieldOrder = 0;
            for (SaveFormLayoutFieldRequest fieldRequest : sectionRequest.fields()) {
                FormLayoutField field = FormLayoutField.builder()
                        .fieldSource(fieldRequest.fieldSource())
                        .standardFieldKey(fieldRequest.standardFieldKey())
                        .customFieldId(fieldRequest.customFieldId())
                        .displayOrder(fieldRequest.displayOrder() != null ? fieldRequest.displayOrder() : fieldOrder)
                        .visible(fieldRequest.visible())
                        .build();
                section.addField(field);
                fieldOrder++;
            }
            layout.addSection(section);
            sectionOrder++;
        }

        FormLayout saved = formLayoutRepository.save(layout);
        return toResponse(saved, customFieldsById);
    }

    private FormLayoutResponse getOrDefault(Long projectId, TargetType targetType) {
        return formLayoutRepository.findByProjectIdAndTargetType(projectId, targetType)
                .map(layout -> toResponse(layout, loadCustomFieldLabels(layout)))
                .orElseGet(() -> buildDefaultResponse(targetType));
    }

    /** §B.1: 표준 필드를 전부 숨길 수는 없다 — 요청에 STANDARD 필드가 하나도 없거나, 전부 visible=false면 거부. */
    private void validateAtLeastOneVisibleStandardField(SaveFormLayoutRequest request) {
        boolean hasVisibleStandardField = request.sections().stream()
                .flatMap(s -> s.fields().stream())
                .anyMatch(f -> f.fieldSource() == FieldSource.STANDARD && Boolean.TRUE.equals(f.visible()));
        if (!hasVisibleStandardField) {
            throw new ValidationException("표준 필드를 전부 숨길 수 없습니다. 최소 1개의 표준 필드는 노출돼야 합니다.");
        }
    }

    private Map<Long, CustomFieldDefinition> validateAndCollectCustomFields(Long projectId, TargetType targetType,
                                                                              SaveFormLayoutRequest request) {
        List<SaveFormLayoutFieldRequest> allFields = request.sections().stream()
                .flatMap(s -> s.fields().stream())
                .toList();

        for (SaveFormLayoutFieldRequest field : allFields) {
            if (field.fieldSource() == FieldSource.STANDARD) {
                if (field.standardFieldKey() == null || field.customFieldId() != null) {
                    throw new ValidationException("STANDARD 필드는 standardFieldKey만 지정해야 합니다.");
                }
                if (!standardFieldKeyRegistry.isValid(targetType, field.standardFieldKey())) {
                    throw new ValidationException(
                            "허용되지 않는 표준 필드 키입니다: " + field.standardFieldKey() + " (targetType=" + targetType + ")");
                }
            } else {
                if (field.customFieldId() == null || field.standardFieldKey() != null) {
                    throw new ValidationException("CUSTOM 필드는 customFieldId만 지정해야 합니다.");
                }
            }
        }

        List<Long> customFieldIds = allFields.stream()
                .filter(f -> f.fieldSource() == FieldSource.CUSTOM)
                .map(SaveFormLayoutFieldRequest::customFieldId)
                .distinct()
                .toList();

        return customFieldIds.stream().collect(Collectors.toMap(id -> id, id -> {
            CustomFieldDefinition definition = customFieldDefinitionRepository.findByIdAndProjectId(id, projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("커스텀 필드를 찾을 수 없습니다: " + id));
            // 크로스 프로젝트/크로스 target_type 오염 방지 (Phase 20 qa-tester 지적 패턴 재사용)
            if (definition.getTargetType() != targetType) {
                throw new ValidationException(
                        "커스텀 필드의 targetType이 레이아웃의 targetType과 일치하지 않습니다: " + id);
            }
            return definition;
        }));
    }

    private Map<Long, CustomFieldDefinition> loadCustomFieldLabels(FormLayout layout) {
        List<Long> customFieldIds = layout.getSections().stream()
                .flatMap(s -> s.getFields().stream())
                .filter(f -> f.getFieldSource() == FieldSource.CUSTOM)
                .map(FormLayoutField::getCustomFieldId)
                .distinct()
                .toList();
        if (customFieldIds.isEmpty()) {
            return Map.of();
        }
        return customFieldDefinitionRepository.findAllById(customFieldIds).stream()
                .collect(Collectors.toMap(CustomFieldDefinition::getId, d -> d));
    }

    private FormLayoutResponse toResponse(FormLayout layout, Map<Long, CustomFieldDefinition> customFieldsById) {
        List<FormLayoutSectionResponse> sections = layout.getSections().stream()
                .map(section -> new FormLayoutSectionResponse(
                        section.getId(),
                        section.getTitle(),
                        section.getDisplayOrder(),
                        section.getFields().stream()
                                .map(field -> toFieldResponse(field, layout.getTargetType(), customFieldsById))
                                .toList()))
                .toList();
        return new FormLayoutResponse(layout.getId(), layout.getTargetType(), sections);
    }

    private FormLayoutFieldResponse toFieldResponse(FormLayoutField field, TargetType targetType,
                                                      Map<Long, CustomFieldDefinition> customFieldsById) {
        if (field.getFieldSource() == FieldSource.STANDARD) {
            return new FormLayoutFieldResponse(
                    field.getId(),
                    FieldSource.STANDARD,
                    field.getStandardFieldKey(),
                    standardFieldKeyRegistry.labelOf(targetType, field.getStandardFieldKey()),
                    null, null, null,
                    field.getDisplayOrder(),
                    field.getVisible());
        }
        CustomFieldDefinition definition = customFieldsById.get(field.getCustomFieldId());
        return new FormLayoutFieldResponse(
                field.getId(),
                FieldSource.CUSTOM,
                null, null,
                field.getCustomFieldId(),
                definition != null ? definition.getLabel() : null,
                definition != null ? definition.getDataType().name() : null,
                field.getDisplayOrder(),
                field.getVisible());
    }

    private FormLayoutResponse buildDefaultResponse(TargetType targetType) {
        List<String> defaultKeys = standardFieldKeyRegistry.defaultOrder(targetType);
        List<FormLayoutFieldResponse> fields = new ArrayList<>();
        int order = 0;
        for (String key : defaultKeys) {
            fields.add(new FormLayoutFieldResponse(
                    null, FieldSource.STANDARD, key, standardFieldKeyRegistry.labelOf(targetType, key),
                    null, null, null, order, Boolean.TRUE));
            order++;
        }
        // §B.3: "표준 필드를 코드 정의 기본 순서로, 섹션 없이 일렬로" — 섹션 1개(제목 없음)로 표현한다.
        FormLayoutSectionResponse defaultSection = new FormLayoutSectionResponse(null, null, 0, fields);
        return new FormLayoutResponse(null, targetType, List.of(defaultSection));
    }
}
