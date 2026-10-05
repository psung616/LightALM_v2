package com.lightalm.enumeration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.lightalm.domain.Priority;
import com.lightalm.domain.Project;
import com.lightalm.domain.SystemRole;
import com.lightalm.domain.User;
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
import com.lightalm.exception.ValidationException;
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
 * ADR-012 §C(Phase 22). base_enum 범위 제한(§C.1), PRIORITY 자동 시드,
 * is_system_default 삭제 거부를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class EnumerationSetServiceTest {

    @Mock
    private ProjectEnumerationSetRepository setRepository;
    @Mock
    private ProjectEnumerationValueRepository valueRepository;
    @Mock
    private ProjectService projectService;
    @Mock
    private ProjectMemberService projectMemberService;

    @InjectMocks
    private EnumerationSetService enumerationSetService;

    private UserPrincipal principal;
    private Project project;

    @BeforeEach
    void setUp() {
        project = Project.builder().id(10L).projectKey("LALM").name("Light ALM").build();
        User user = User.builder()
                .id(1L).username("admin1").password("hash").email("admin1@example.com")
                .fullName("Admin One").systemRole(SystemRole.USER).enabled(true).build();
        principal = new UserPrincipal(user);
    }

    @Test
    void create_withRequirementStatusBaseEnum_isRejected() {
        CreateEnumerationSetRequest request =
                new CreateEnumerationSetRequest("REQ_STATUS_EXT", BaseEnumType.REQUIREMENT_STATUS, "요구사항 상태 확장");

        assertThatThrownBy(() -> enumerationSetService.create(10L, request, principal))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void create_withIssueStatusBaseEnum_isRejected() {
        CreateEnumerationSetRequest request =
                new CreateEnumerationSetRequest("ISSUE_STATUS_EXT", BaseEnumType.ISSUE_STATUS, "이슈 상태 확장");

        assertThatThrownBy(() -> enumerationSetService.create(10L, request, principal))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void create_withTestCaseStatusBaseEnum_isRejected() {
        CreateEnumerationSetRequest request =
                new CreateEnumerationSetRequest("TC_STATUS_EXT", BaseEnumType.TEST_CASE_STATUS, "테스트케이스 상태 확장");

        assertThatThrownBy(() -> enumerationSetService.create(10L, request, principal))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void create_withPriorityBaseEnum_seedsFourSystemDefaultValues() {
        CreateEnumerationSetRequest request = new CreateEnumerationSetRequest("PRIORITY", BaseEnumType.PRIORITY, "우선순위 확장");

        when(setRepository.existsByProjectIdAndEnumKey(10L, "PRIORITY")).thenReturn(false);
        when(projectService.getEntity(10L)).thenReturn(project);
        when(setRepository.save(any(ProjectEnumerationSet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(valueRepository.save(any(ProjectEnumerationValue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EnumerationSetResponse response = enumerationSetService.create(10L, request, principal);

        assertThat(response.values()).hasSize(Priority.values().length);
        assertThat(response.values()).allSatisfy(value -> assertThat(value.isSystemDefault()).isTrue());
        assertThat(response.values().stream().map(EnumerationValueResponse::valueKey).toList())
                .containsExactlyInAnyOrder("LOW", "MEDIUM", "HIGH", "CRITICAL");
    }

    @Test
    void create_withNullBaseEnum_doesNotSeedAnyValue() {
        CreateEnumerationSetRequest request = new CreateEnumerationSetRequest("SEVERITY", null, "커스텀 심각도");

        when(setRepository.existsByProjectIdAndEnumKey(10L, "SEVERITY")).thenReturn(false);
        when(projectService.getEntity(10L)).thenReturn(project);
        when(setRepository.save(any(ProjectEnumerationSet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EnumerationSetResponse response = enumerationSetService.create(10L, request, principal);

        assertThat(response.values()).isEmpty();
    }

    @Test
    void create_whenEnumKeyAlreadyExists_throwsValidationException() {
        CreateEnumerationSetRequest request = new CreateEnumerationSetRequest("PRIORITY", BaseEnumType.PRIORITY, "우선순위 확장");
        when(setRepository.existsByProjectIdAndEnumKey(10L, "PRIORITY")).thenReturn(true);

        assertThatThrownBy(() -> enumerationSetService.create(10L, request, principal))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void deprecateValue_whenSystemDefault_isRejected() {
        ProjectEnumerationSet set = ProjectEnumerationSet.builder()
                .project(project).enumKey("PRIORITY").baseEnum(BaseEnumType.PRIORITY).name("우선순위 확장").build();
        ProjectEnumerationValue systemDefault = ProjectEnumerationValue.builder()
                .enumerationSet(set).valueKey("MEDIUM").label("MEDIUM").displayOrder(1).isSystemDefault(true).build();

        when(setRepository.findByIdAndProjectId(1L, 10L)).thenReturn(Optional.of(set));
        when(valueRepository.findByIdAndEnumerationSetId(2L, 1L)).thenReturn(Optional.of(systemDefault));

        assertThatThrownBy(() -> enumerationSetService.deprecateValue(10L, 1L, 2L, principal))
                .isInstanceOf(ValidationException.class);
        assertThat(systemDefault.isActive()).isTrue();
    }

    @Test
    void deprecateValue_whenNotSystemDefault_succeeds() {
        ProjectEnumerationSet set = ProjectEnumerationSet.builder()
                .project(project).enumKey("PRIORITY").baseEnum(BaseEnumType.PRIORITY).name("우선순위 확장").build();
        ProjectEnumerationValue custom = ProjectEnumerationValue.builder()
                .enumerationSet(set).valueKey("BLOCKER").label("Blocker").displayOrder(5).isSystemDefault(false).build();

        when(setRepository.findByIdAndProjectId(1L, 10L)).thenReturn(Optional.of(set));
        when(valueRepository.findByIdAndEnumerationSetId(3L, 1L)).thenReturn(Optional.of(custom));

        enumerationSetService.deprecateValue(10L, 1L, 3L, principal);

        assertThat(custom.getStatus()).isEqualTo(EnumerationValueStatus.DEPRECATED);
    }

    @Test
    void addValue_whenValueKeyAlreadyExists_throwsValidationException() {
        ProjectEnumerationSet set = ProjectEnumerationSet.builder()
                .project(project).enumKey("SEVERITY").baseEnum(null).name("심각도").build();
        when(setRepository.findByIdAndProjectId(1L, 10L)).thenReturn(Optional.of(set));
        when(valueRepository.existsByEnumerationSetIdAndValueKey(1L, "BLOCKER")).thenReturn(true);

        CreateEnumerationValueRequest request = new CreateEnumerationValueRequest("BLOCKER", "Blocker", null);

        assertThatThrownBy(() -> enumerationSetService.addValue(10L, 1L, request, principal))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void updateValue_changesLabelAndOrder_evenWhenSystemDefault() {
        ProjectEnumerationSet set = ProjectEnumerationSet.builder()
                .project(project).enumKey("PRIORITY").baseEnum(BaseEnumType.PRIORITY).name("우선순위 확장").build();
        ProjectEnumerationValue systemDefault = ProjectEnumerationValue.builder()
                .enumerationSet(set).valueKey("MEDIUM").label("MEDIUM").displayOrder(1).isSystemDefault(true).build();

        when(setRepository.findByIdAndProjectId(1L, 10L)).thenReturn(Optional.of(set));
        when(valueRepository.findByIdAndEnumerationSetId(2L, 1L)).thenReturn(Optional.of(systemDefault));

        EnumerationValueResponse response =
                enumerationSetService.updateValue(10L, 1L, 2L, new UpdateEnumerationValueRequest("중간", 9), principal);

        assertThat(response.label()).isEqualTo("중간");
        assertThat(response.displayOrder()).isEqualTo(9);
        assertThat(response.isSystemDefault()).isTrue();
    }

    @Test
    void listActiveValues_whenNoSetExistsForPriority_returnsDefaultJavaEnumValues() {
        when(setRepository.findByProjectIdAndEnumKey(10L, "PRIORITY")).thenReturn(Optional.empty());

        List<EnumerationValueResponse> values = enumerationSetService.listActiveValues(10L, "PRIORITY", principal);

        assertThat(values).hasSize(Priority.values().length);
        assertThat(values.stream().map(EnumerationValueResponse::valueKey).toList())
                .containsExactlyInAnyOrder("LOW", "MEDIUM", "HIGH", "CRITICAL");
    }

    @Test
    void listActiveValues_whenNoSetExistsForUnknownEnumKey_returnsEmptyList() {
        when(setRepository.findByProjectIdAndEnumKey(10L, "SEVERITY")).thenReturn(Optional.empty());

        List<EnumerationValueResponse> values = enumerationSetService.listActiveValues(10L, "SEVERITY", principal);

        assertThat(values).isEmpty();
    }
}
