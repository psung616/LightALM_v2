package com.lightalm.customfield.service;

import com.lightalm.customfield.domain.CustomFieldDefinition;
import com.lightalm.customfield.domain.CustomFieldStatus;
import com.lightalm.customfield.dto.CreateCustomFieldDefinitionRequest;
import com.lightalm.customfield.dto.CustomFieldDefinitionDetailResponse;
import com.lightalm.customfield.dto.CustomFieldDefinitionSummaryResponse;
import com.lightalm.customfield.dto.UpdateCustomFieldDefinitionRequest;
import com.lightalm.customfield.repository.CustomFieldDefinitionRepository;
import com.lightalm.domain.Project;
import com.lightalm.domain.ProjectRole;
import com.lightalm.domain.TargetType;
import com.lightalm.domain.User;
import com.lightalm.enumeration.repository.ProjectEnumerationSetRepository;
import com.lightalm.exception.ResourceNotFoundException;
import com.lightalm.exception.ValidationException;
import com.lightalm.repository.UserRepository;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.ProjectMemberService;
import com.lightalm.service.ProjectService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ADR-012 §A. 커스텀 필드 정의 생성/수정/소프트삭제 — 쓰기는 모두 PROJECT_ADMIN+.
 */
@Service
@RequiredArgsConstructor
public class CustomFieldDefinitionService {

    private final CustomFieldDefinitionRepository customFieldDefinitionRepository;
    private final ProjectService projectService;
    private final ProjectMemberService projectMemberService;
    private final UserRepository userRepository;
    private final ProjectEnumerationSetRepository projectEnumerationSetRepository;

    @Transactional(readOnly = true)
    public List<CustomFieldDefinitionDetailResponse> listForConfig(Long projectId, TargetType targetType, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.PROJECT_ADMIN);
        return customFieldDefinitionRepository.findByProjectIdAndTargetTypeOrderByDisplayOrderAsc(projectId, targetType)
                .stream()
                .map(CustomFieldDefinitionDetailResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CustomFieldDefinitionSummaryResponse> listActive(Long projectId, TargetType targetType, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.VIEWER);
        return customFieldDefinitionRepository
                .findByProjectIdAndTargetTypeAndStatusOrderByDisplayOrderAsc(projectId, targetType, CustomFieldStatus.ACTIVE)
                .stream()
                .map(CustomFieldDefinitionSummaryResponse::from)
                .toList();
    }

    @Transactional
    public CustomFieldDefinitionDetailResponse create(Long projectId, CreateCustomFieldDefinitionRequest request,
                                                        UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.PROJECT_ADMIN);
        if (customFieldDefinitionRepository.existsByProjectIdAndTargetTypeAndFieldKey(
                projectId, request.targetType(), request.fieldKey())) {
            throw new ValidationException(
                    "이미 같은 targetType/fieldKey의 필드가 존재합니다: " + request.targetType() + "/" + request.fieldKey());
        }
        if (request.enumerationSetId() != null
                && !projectEnumerationSetRepository.existsByIdAndProjectId(request.enumerationSetId(), projectId)) {
            throw new ValidationException(
                    "존재하지 않거나 다른 프로젝트에 속한 열거형 집합입니다: " + request.enumerationSetId());
        }

        Project project = projectService.getEntity(projectId);
        User createdBy = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + principal.getId()));

        int nextDisplayOrder = customFieldDefinitionRepository
                .findByProjectIdAndTargetTypeOrderByDisplayOrderAsc(projectId, request.targetType())
                .stream()
                .mapToInt(CustomFieldDefinition::getDisplayOrder)
                .max()
                .orElse(-1) + 1;

        CustomFieldDefinition definition = CustomFieldDefinition.builder()
                .project(project)
                .targetType(request.targetType())
                .fieldKey(request.fieldKey())
                .label(request.label())
                .dataType(request.dataType())
                .enumerationSetId(request.enumerationSetId())
                .required(request.required())
                .defaultValue(request.defaultValue())
                .displayOrder(nextDisplayOrder)
                .createdBy(createdBy)
                .build();

        return CustomFieldDefinitionDetailResponse.from(customFieldDefinitionRepository.save(definition));
    }

    @Transactional
    public CustomFieldDefinitionDetailResponse update(Long projectId, Long fieldId, UpdateCustomFieldDefinitionRequest request,
                                                        UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.PROJECT_ADMIN);
        CustomFieldDefinition definition = getScoped(projectId, fieldId);
        definition.updateDetails(request.label(), request.required(), request.defaultValue(), request.displayOrder());
        return CustomFieldDefinitionDetailResponse.from(definition);
    }

    @Transactional
    public void deprecate(Long projectId, Long fieldId, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.PROJECT_ADMIN);
        CustomFieldDefinition definition = getScoped(projectId, fieldId);
        definition.deprecate();
    }

    private CustomFieldDefinition getScoped(Long projectId, Long fieldId) {
        return customFieldDefinitionRepository.findByIdAndProjectId(fieldId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("커스텀 필드를 찾을 수 없습니다: " + fieldId));
    }
}
