package com.lightalm.customfield.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.lightalm.customfield.domain.CustomFieldDataType;
import com.lightalm.customfield.domain.CustomFieldDefinition;
import com.lightalm.customfield.domain.CustomFieldStatus;
import com.lightalm.customfield.dto.CreateCustomFieldDefinitionRequest;
import com.lightalm.customfield.dto.CustomFieldDefinitionDetailResponse;
import com.lightalm.customfield.dto.UpdateCustomFieldDefinitionRequest;
import com.lightalm.customfield.repository.CustomFieldDefinitionRepository;
import com.lightalm.domain.Project;
import com.lightalm.domain.SystemRole;
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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * ADR-012 §A(Phase 20) 커스텀 필드 정의 서비스. 생성/수정(불변 필드 보존)/소프트삭제,
 * UNIQUE(project_id,target_type,field_key) 위반 시 예외를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class CustomFieldDefinitionServiceTest {

    @Mock
    private CustomFieldDefinitionRepository customFieldDefinitionRepository;
    @Mock
    private ProjectService projectService;
    @Mock
    private ProjectMemberService projectMemberService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProjectEnumerationSetRepository projectEnumerationSetRepository;

    @InjectMocks
    private CustomFieldDefinitionService customFieldDefinitionService;

    private UserPrincipal principal;
    private Project project;
    private User user;

    @BeforeEach
    void setUp() {
        project = Project.builder().id(10L).projectKey("LALM").name("Light ALM").build();
        user = User.builder()
                .id(1L).username("admin1").password("hash").email("admin1@example.com")
                .fullName("Admin One").systemRole(SystemRole.USER).enabled(true).build();
        principal = new UserPrincipal(user);
    }

    @Test
    void create_success_savesDefinitionWithActiveStatus() {
        CreateCustomFieldDefinitionRequest request = new CreateCustomFieldDefinitionRequest(
                TargetType.REQUIREMENT, "severity_custom", "커스텀 심각도", CustomFieldDataType.TEXT, null, true, null);

        when(customFieldDefinitionRepository.existsByProjectIdAndTargetTypeAndFieldKey(
                10L, TargetType.REQUIREMENT, "severity_custom")).thenReturn(false);
        when(customFieldDefinitionRepository.findByProjectIdAndTargetTypeOrderByDisplayOrderAsc(10L, TargetType.REQUIREMENT))
                .thenReturn(List.of());
        when(projectService.getEntity(10L)).thenReturn(project);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(customFieldDefinitionRepository.save(any(CustomFieldDefinition.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CustomFieldDefinitionDetailResponse response = customFieldDefinitionService.create(10L, request, principal);

        assertThat(response.fieldKey()).isEqualTo("severity_custom");
        assertThat(response.dataType()).isEqualTo(CustomFieldDataType.TEXT);
        assertThat(response.status()).isEqualTo(CustomFieldStatus.ACTIVE);
        assertThat(response.displayOrder()).isZero();
    }

    @Test
    void create_whenFieldKeyAlreadyExistsForSameProjectAndTargetType_throwsValidationException() {
        CreateCustomFieldDefinitionRequest request = new CreateCustomFieldDefinitionRequest(
                TargetType.REQUIREMENT, "severity_custom", "커스텀 심각도", CustomFieldDataType.TEXT, null, false, null);

        when(customFieldDefinitionRepository.existsByProjectIdAndTargetTypeAndFieldKey(
                10L, TargetType.REQUIREMENT, "severity_custom")).thenReturn(true);

        assertThatThrownBy(() -> customFieldDefinitionService.create(10L, request, principal))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void create_withEnumerationSetIdBelongingToSameProject_savesSuccessfully() {
        CreateCustomFieldDefinitionRequest request = new CreateCustomFieldDefinitionRequest(
                TargetType.REQUIREMENT, "severity_custom", "커스텀 심각도", CustomFieldDataType.SINGLE_SELECT, 42L, true, null);

        when(customFieldDefinitionRepository.existsByProjectIdAndTargetTypeAndFieldKey(
                10L, TargetType.REQUIREMENT, "severity_custom")).thenReturn(false);
        when(projectEnumerationSetRepository.existsByIdAndProjectId(42L, 10L)).thenReturn(true);
        when(customFieldDefinitionRepository.findByProjectIdAndTargetTypeOrderByDisplayOrderAsc(10L, TargetType.REQUIREMENT))
                .thenReturn(List.of());
        when(projectService.getEntity(10L)).thenReturn(project);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(customFieldDefinitionRepository.save(any(CustomFieldDefinition.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CustomFieldDefinitionDetailResponse response = customFieldDefinitionService.create(10L, request, principal);

        assertThat(response.enumerationSetId()).isEqualTo(42L);
    }

    @Test
    void create_whenEnumerationSetIdDoesNotBelongToProject_throwsValidationExceptionNotFkViolation() {
        CreateCustomFieldDefinitionRequest request = new CreateCustomFieldDefinitionRequest(
                TargetType.REQUIREMENT, "severity_custom", "커스텀 심각도", CustomFieldDataType.SINGLE_SELECT, 999999L, true, null);

        when(customFieldDefinitionRepository.existsByProjectIdAndTargetTypeAndFieldKey(
                10L, TargetType.REQUIREMENT, "severity_custom")).thenReturn(false);
        when(projectEnumerationSetRepository.existsByIdAndProjectId(999999L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> customFieldDefinitionService.create(10L, request, principal))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void update_changesLabelRequiredDefaultValueDisplayOrder_butNotDataTypeOrFieldKey() {
        CustomFieldDefinition existing = CustomFieldDefinition.builder()
                .project(project).targetType(TargetType.ISSUE).fieldKey("blocker_reason")
                .label("차단 이유").dataType(CustomFieldDataType.TEXT).required(false).displayOrder(0)
                .build();

        when(customFieldDefinitionRepository.findByIdAndProjectId(5L, 10L)).thenReturn(Optional.of(existing));

        UpdateCustomFieldDefinitionRequest request =
                new UpdateCustomFieldDefinitionRequest("변경된 라벨", true, "기본값", 3);

        CustomFieldDefinitionDetailResponse response =
                customFieldDefinitionService.update(10L, 5L, request, principal);

        assertThat(response.label()).isEqualTo("변경된 라벨");
        assertThat(response.required()).isTrue();
        assertThat(response.defaultValue()).isEqualTo("기본값");
        assertThat(response.displayOrder()).isEqualTo(3);
        // dataType/fieldKey는 요청 DTO 자체에 없으므로 생성 당시 값 그대로 유지된다.
        assertThat(response.dataType()).isEqualTo(CustomFieldDataType.TEXT);
        assertThat(response.fieldKey()).isEqualTo("blocker_reason");
    }

    @Test
    void update_whenFieldNotFoundInProject_throwsResourceNotFound() {
        when(customFieldDefinitionRepository.findByIdAndProjectId(99L, 10L)).thenReturn(Optional.empty());

        UpdateCustomFieldDefinitionRequest request = new UpdateCustomFieldDefinitionRequest("라벨", false, null, 0);

        assertThatThrownBy(() -> customFieldDefinitionService.update(10L, 99L, request, principal))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deprecate_setsStatusToDeprecated_softDeleteNotHardDelete() {
        CustomFieldDefinition existing = CustomFieldDefinition.builder()
                .project(project).targetType(TargetType.TEST_CASE).fieldKey("auto_runner")
                .label("자동화 러너").dataType(CustomFieldDataType.BOOLEAN).required(false).displayOrder(0)
                .build();
        when(customFieldDefinitionRepository.findByIdAndProjectId(7L, 10L)).thenReturn(Optional.of(existing));

        customFieldDefinitionService.deprecate(10L, 7L, principal);

        assertThat(existing.getStatus()).isEqualTo(CustomFieldStatus.DEPRECATED);
        assertThat(existing.isActive()).isFalse();
    }
}
