package com.lightalm.formlayout.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.lightalm.customfield.domain.CustomFieldDataType;
import com.lightalm.customfield.domain.CustomFieldDefinition;
import com.lightalm.customfield.repository.CustomFieldDefinitionRepository;
import com.lightalm.domain.Project;
import com.lightalm.domain.SystemRole;
import com.lightalm.domain.TargetType;
import com.lightalm.domain.User;
import com.lightalm.exception.ResourceNotFoundException;
import com.lightalm.exception.ValidationException;
import com.lightalm.formlayout.domain.FieldSource;
import com.lightalm.formlayout.domain.FormLayout;
import com.lightalm.formlayout.dto.FormLayoutResponse;
import com.lightalm.formlayout.dto.SaveFormLayoutFieldRequest;
import com.lightalm.formlayout.dto.SaveFormLayoutRequest;
import com.lightalm.formlayout.dto.SaveFormLayoutSectionRequest;
import com.lightalm.formlayout.repository.FormLayoutRepository;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.ProjectMemberService;
import com.lightalm.service.ProjectService;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * ADR-012 §B(Phase 21) 폼 레이아웃 서비스. 기본 레이아웃 하위호환, 전체 치환 저장,
 * standardFieldKey 화이트리스트 검증, "표준 필드 전부 숨김 금지" 제약, 커스텀 필드의
 * 프로젝트+targetType 소속 검증을 다룬다.
 */
@ExtendWith(MockitoExtension.class)
class FormLayoutServiceTest {

    @Mock
    private FormLayoutRepository formLayoutRepository;
    @Mock
    private CustomFieldDefinitionRepository customFieldDefinitionRepository;
    @Mock
    private ProjectMemberService projectMemberService;
    @Mock
    private ProjectService projectService;

    private StandardFieldKeyRegistry standardFieldKeyRegistry;

    @InjectMocks
    private FormLayoutService formLayoutService;

    private UserPrincipal principal;
    private Project project;

    @BeforeEach
    void setUp() throws Exception {
        project = Project.builder().id(10L).projectKey("LALM").name("Light ALM").build();
        User user = User.builder()
                .id(1L).username("admin1").password("hash").email("admin1@example.com")
                .fullName("Admin One").systemRole(SystemRole.USER).enabled(true).build();
        principal = new UserPrincipal(user);

        // @InjectMocks가 StandardFieldKeyRegistry도 Mockito @Mock으로 주입하므로, 실제 화이트리스트
        // 로직이 필요한 이 테스트에서는 실제 인스턴스로 교체한다(CustomFieldValueServiceTest의
        // ObjectMapper 치환 패턴과 동일).
        standardFieldKeyRegistry = new StandardFieldKeyRegistry();
        Field registryField = FormLayoutService.class.getDeclaredField("standardFieldKeyRegistry");
        registryField.setAccessible(true);
        registryField.set(formLayoutService, standardFieldKeyRegistry);
    }

    @Test
    void getForRender_whenNoLayoutConfigured_returnsDefaultLayoutWithStandardFieldsInRegistryOrder() {
        when(formLayoutRepository.findByProjectIdAndTargetType(10L, TargetType.ISSUE)).thenReturn(Optional.empty());

        FormLayoutResponse response = formLayoutService.getForRender(10L, TargetType.ISSUE, principal);

        assertThat(response.id()).isNull();
        assertThat(response.sections()).hasSize(1);
        assertThat(response.sections().get(0).fields())
                .extracting(f -> f.standardFieldKey())
                .containsExactlyElementsOf(standardFieldKeyRegistry.defaultOrder(TargetType.ISSUE));
        assertThat(response.sections().get(0).fields()).allMatch(f -> f.visible());
    }

    @Test
    void saveLayout_success_persistsSectionsAndFieldsInOrder() {
        when(formLayoutRepository.findByProjectIdAndTargetType(10L, TargetType.REQUIREMENT)).thenReturn(Optional.empty());
        when(projectService.getEntity(10L)).thenReturn(project);
        when(formLayoutRepository.save(any(FormLayout.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SaveFormLayoutRequest request = new SaveFormLayoutRequest(List.of(
                new SaveFormLayoutSectionRequest("기본 정보", 0, List.of(
                        new SaveFormLayoutFieldRequest(FieldSource.STANDARD, "title", null, 0, true),
                        new SaveFormLayoutFieldRequest(FieldSource.STANDARD, "priority", null, 1, true)))));

        FormLayoutResponse response = formLayoutService.saveLayout(10L, TargetType.REQUIREMENT, request, principal);

        assertThat(response.sections()).hasSize(1);
        assertThat(response.sections().get(0).title()).isEqualTo("기본 정보");
        assertThat(response.sections().get(0).fields()).hasSize(2);
        assertThat(response.sections().get(0).fields().get(0).standardFieldKey()).isEqualTo("title");
        assertThat(response.sections().get(0).fields().get(1).standardFieldKey()).isEqualTo("priority");
    }

    @Test
    void saveLayout_whenStandardFieldKeyNotInWhitelist_throwsValidationException() {
        SaveFormLayoutRequest request = new SaveFormLayoutRequest(List.of(
                new SaveFormLayoutSectionRequest("기본 정보", 0, List.of(
                        new SaveFormLayoutFieldRequest(FieldSource.STANDARD, "notARealField", null, 0, true)))));

        assertThatThrownBy(() -> formLayoutService.saveLayout(10L, TargetType.REQUIREMENT, request, principal))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void saveLayout_whenAllStandardFieldsHidden_throwsValidationException() {
        SaveFormLayoutRequest request = new SaveFormLayoutRequest(List.of(
                new SaveFormLayoutSectionRequest("기본 정보", 0, List.of(
                        new SaveFormLayoutFieldRequest(FieldSource.STANDARD, "title", null, 0, false),
                        new SaveFormLayoutFieldRequest(FieldSource.STANDARD, "priority", null, 1, false)))));

        assertThatThrownBy(() -> formLayoutService.saveLayout(10L, TargetType.REQUIREMENT, request, principal))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("표준 필드");
    }

    @Test
    void saveLayout_whenCustomFieldBelongsToDifferentProject_throwsResourceNotFound() {
        when(customFieldDefinitionRepository.findByIdAndProjectId(99L, 10L)).thenReturn(Optional.empty());

        SaveFormLayoutRequest request = new SaveFormLayoutRequest(List.of(
                new SaveFormLayoutSectionRequest("추가 정보", 0, List.of(
                        new SaveFormLayoutFieldRequest(FieldSource.STANDARD, "title", null, 0, true),
                        new SaveFormLayoutFieldRequest(FieldSource.CUSTOM, null, 99L, 1, true)))));

        assertThatThrownBy(() -> formLayoutService.saveLayout(10L, TargetType.REQUIREMENT, request, principal))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void saveLayout_whenCustomFieldBelongsToDifferentTargetType_throwsValidationException() {
        CustomFieldDefinition otherTargetTypeField = CustomFieldDefinition.builder()
                .project(project).targetType(TargetType.ISSUE).fieldKey("blocker_reason")
                .label("차단 이유").dataType(CustomFieldDataType.TEXT).required(false).displayOrder(0)
                .build();
        when(customFieldDefinitionRepository.findByIdAndProjectId(5L, 10L)).thenReturn(Optional.of(otherTargetTypeField));

        SaveFormLayoutRequest request = new SaveFormLayoutRequest(List.of(
                new SaveFormLayoutSectionRequest("추가 정보", 0, List.of(
                        new SaveFormLayoutFieldRequest(FieldSource.STANDARD, "title", null, 0, true),
                        new SaveFormLayoutFieldRequest(FieldSource.CUSTOM, null, 5L, 1, true)))));

        // request의 targetType은 REQUIREMENT인데 커스텀 필드는 ISSUE 소속 -> 거부
        assertThatThrownBy(() -> formLayoutService.saveLayout(10L, TargetType.REQUIREMENT, request, principal))
                .isInstanceOf(ValidationException.class);
    }
}
