package com.lightalm.customfield.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lightalm.customfield.domain.CustomFieldDataType;
import com.lightalm.customfield.domain.CustomFieldDefinition;
import com.lightalm.customfield.domain.CustomFieldStatus;
import com.lightalm.customfield.domain.CustomFieldValue;
import com.lightalm.customfield.dto.BulkSaveCustomFieldValuesRequest;
import com.lightalm.customfield.dto.CustomFieldValueItem;
import com.lightalm.customfield.dto.CustomFieldValueResponse;
import com.lightalm.customfield.repository.CustomFieldDefinitionRepository;
import com.lightalm.customfield.repository.CustomFieldValueRepository;
import com.lightalm.domain.Project;
import com.lightalm.domain.SystemRole;
import com.lightalm.domain.TargetType;
import com.lightalm.domain.User;
import com.lightalm.exception.ResourceNotFoundException;
import com.lightalm.exception.ValidationException;
import com.lightalm.repository.UserRepository;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.ProjectMemberService;
import com.lightalm.service.support.PolymorphicTargetValidator;
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
 * ADR-012 §A(Phase 20) 커스텀 필드 값 서비스. MULTI_SELECT의 JSON 배열 문자열 저장/조회,
 * 존재하지 않는 대상에 대한 PolymorphicTargetValidator 차단, 소프트 삭제된 필드가
 * 값이 있을 때만 상세 조회에 남는지를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class CustomFieldValueServiceTest {

    @Mock
    private CustomFieldValueRepository customFieldValueRepository;
    @Mock
    private CustomFieldDefinitionRepository customFieldDefinitionRepository;
    @Mock
    private ProjectMemberService projectMemberService;
    @Mock
    private PolymorphicTargetValidator polymorphicTargetValidator;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomFieldValueService customFieldValueService;

    private UserPrincipal principal;
    private Project project;
    private User user;

    @BeforeEach
    void setUp() throws Exception {
        project = Project.builder().id(10L).projectKey("LALM").name("Light ALM").build();
        user = User.builder()
                .id(1L).username("member1").password("hash").email("member1@example.com")
                .fullName("Member One").systemRole(SystemRole.USER).enabled(true).build();
        principal = new UserPrincipal(user);

        // @InjectMocks가 ObjectMapper 필드도 Mockito @Mock으로 주입하므로, 실제 직렬화/역직렬화가
        // 필요한 이 테스트에서는 실제 ObjectMapper 인스턴스로 교체한다.
        Field objectMapperField = CustomFieldValueService.class.getDeclaredField("objectMapper");
        objectMapperField.setAccessible(true);
        objectMapperField.set(customFieldValueService, new ObjectMapper());
    }

    private CustomFieldDefinition multiSelectField(Long id) {
        CustomFieldDefinition definition = CustomFieldDefinition.builder()
                .project(project).targetType(TargetType.ISSUE).fieldKey("affected_components")
                .label("영향 받는 컴포넌트").dataType(CustomFieldDataType.MULTI_SELECT).required(false).displayOrder(0)
                .build();
        setId(definition, id);
        return definition;
    }

    private void setId(Object entity, Long id) {
        try {
            Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void saveValues_multiSelect_withValidJsonArray_isPersistedAsJsonArrayString() {
        CustomFieldDefinition field = multiSelectField(1L);
        String jsonArrayValue = "[\"API\",\"DB\"]";

        when(customFieldDefinitionRepository.findByIdAndProjectId(1L, 10L)).thenReturn(Optional.of(field));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(customFieldValueRepository.findByField_IdAndTargetTypeAndTargetId(1L, TargetType.ISSUE, 100L))
                .thenReturn(Optional.empty());
        when(customFieldDefinitionRepository.findByProjectIdAndTargetTypeAndStatusOrderByDisplayOrderAsc(
                10L, TargetType.ISSUE, CustomFieldStatus.ACTIVE)).thenReturn(List.of(field));
        when(customFieldDefinitionRepository.findByProjectIdAndTargetTypeAndStatusOrderByDisplayOrderAsc(
                10L, TargetType.ISSUE, CustomFieldStatus.DEPRECATED)).thenReturn(List.of());

        CustomFieldValue savedValue = CustomFieldValue.builder()
                .field(field).targetType(TargetType.ISSUE).targetId(100L).value(jsonArrayValue).updatedBy(user).build();
        when(customFieldValueRepository.save(any(CustomFieldValue.class))).thenReturn(savedValue);
        when(customFieldValueRepository.findByField_Project_IdAndTargetTypeAndTargetId(10L, TargetType.ISSUE, 100L))
                .thenReturn(List.of(savedValue));

        BulkSaveCustomFieldValuesRequest request =
                new BulkSaveCustomFieldValuesRequest(List.of(new CustomFieldValueItem(1L, jsonArrayValue)));

        List<CustomFieldValueResponse> result = customFieldValueService.saveValues(10L, TargetType.ISSUE, 100L, request, principal);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).value()).isEqualTo(jsonArrayValue);
        assertThat(result.get(0).dataType()).isEqualTo(CustomFieldDataType.MULTI_SELECT);
    }

    @Test
    void saveValues_multiSelect_withNonArrayJson_throwsValidationException() {
        CustomFieldDefinition field = multiSelectField(1L);
        when(customFieldDefinitionRepository.findByIdAndProjectId(1L, 10L)).thenReturn(Optional.of(field));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        BulkSaveCustomFieldValuesRequest request =
                new BulkSaveCustomFieldValuesRequest(List.of(new CustomFieldValueItem(1L, "{\"not\":\"an array\"}")));

        assertThatThrownBy(() -> customFieldValueService.saveValues(10L, TargetType.ISSUE, 100L, request, principal))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void saveValues_whenTargetDoesNotExist_polymorphicTargetValidatorBlocksBeforeAnyWrite() {
        doThrow(new ResourceNotFoundException("이슈를 찾을 수 없습니다: 999"))
                .when(polymorphicTargetValidator).ensureExists(10L, TargetType.ISSUE, 999L);

        BulkSaveCustomFieldValuesRequest request =
                new BulkSaveCustomFieldValuesRequest(List.of(new CustomFieldValueItem(1L, "x")));

        assertThatThrownBy(() -> customFieldValueService.saveValues(10L, TargetType.ISSUE, 999L, request, principal))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listValues_whenTargetDoesNotExist_polymorphicTargetValidatorBlocks() {
        doThrow(new ResourceNotFoundException("이슈를 찾을 수 없습니다: 999"))
                .when(polymorphicTargetValidator).ensureExists(10L, TargetType.ISSUE, 999L);

        assertThatThrownBy(() -> customFieldValueService.listValues(10L, TargetType.ISSUE, 999L, principal))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listValues_deprecatedFieldWithExistingValue_isIncluded_butDeprecatedFieldWithoutValue_isExcluded() {
        CustomFieldDefinition activeField = CustomFieldDefinition.builder()
                .project(project).targetType(TargetType.ISSUE).fieldKey("active_field")
                .label("활성 필드").dataType(CustomFieldDataType.TEXT).required(false).displayOrder(0)
                .build();
        setId(activeField, 1L);

        CustomFieldDefinition deprecatedWithValue = CustomFieldDefinition.builder()
                .project(project).targetType(TargetType.ISSUE).fieldKey("deprecated_with_value")
                .label("비활성(값 있음)").dataType(CustomFieldDataType.TEXT).required(false).displayOrder(1)
                .build();
        setId(deprecatedWithValue, 2L);
        deprecatedWithValue.deprecate();

        CustomFieldDefinition deprecatedWithoutValue = CustomFieldDefinition.builder()
                .project(project).targetType(TargetType.ISSUE).fieldKey("deprecated_without_value")
                .label("비활성(값 없음)").dataType(CustomFieldDataType.TEXT).required(false).displayOrder(2)
                .build();
        setId(deprecatedWithoutValue, 3L);
        deprecatedWithoutValue.deprecate();

        CustomFieldValue existingValue = CustomFieldValue.builder()
                .field(deprecatedWithValue).targetType(TargetType.ISSUE).targetId(100L).value("legacy-value").build();

        when(customFieldDefinitionRepository.findByProjectIdAndTargetTypeAndStatusOrderByDisplayOrderAsc(
                10L, TargetType.ISSUE, CustomFieldStatus.ACTIVE)).thenReturn(List.of(activeField));
        when(customFieldDefinitionRepository.findByProjectIdAndTargetTypeAndStatusOrderByDisplayOrderAsc(
                10L, TargetType.ISSUE, CustomFieldStatus.DEPRECATED))
                .thenReturn(List.of(deprecatedWithValue, deprecatedWithoutValue));
        when(customFieldValueRepository.findByField_Project_IdAndTargetTypeAndTargetId(10L, TargetType.ISSUE, 100L))
                .thenReturn(List.of(existingValue));

        List<CustomFieldValueResponse> result = customFieldValueService.listValues(10L, TargetType.ISSUE, 100L, principal);

        assertThat(result).extracting(CustomFieldValueResponse::fieldKey)
                .containsExactly("active_field", "deprecated_with_value");
        assertThat(result).noneMatch(r -> r.fieldKey().equals("deprecated_without_value"));

        CustomFieldValueResponse activeFieldResponse = result.stream()
                .filter(r -> r.fieldKey().equals("active_field")).findFirst().orElseThrow();
        assertThat(activeFieldResponse.value()).isNull();

        CustomFieldValueResponse deprecatedResponse = result.stream()
                .filter(r -> r.fieldKey().equals("deprecated_with_value")).findFirst().orElseThrow();
        assertThat(deprecatedResponse.value()).isEqualTo("legacy-value");
        assertThat(deprecatedResponse.status()).isEqualTo(CustomFieldStatus.DEPRECATED);
    }

    /**
     * qa-tester 반려 사유 수정(2026-10-05): ADR-012 §A.3의 "비활성 필드는 생성/수정 폼에서는
     * 숨긴다"는 "폼에 새 입력 옵션으로 내놓지 않는다"는 뜻일 뿐, 값 저장 API 자체를 영구 잠그는
     * 것이 아니다. 이 필드에 아직 값이 없는 상태(신규 생성)에서만 거부해야 한다.
     */
    @Test
    void saveValues_whenFieldIsDeprecatedAndNoExistingValue_rejectsNewValueCreation() {
        CustomFieldDefinition deprecatedField = CustomFieldDefinition.builder()
                .project(project).targetType(TargetType.ISSUE).fieldKey("legacy_field")
                .label("레거시 필드").dataType(CustomFieldDataType.TEXT).required(false).displayOrder(0)
                .build();
        setId(deprecatedField, 1L);
        deprecatedField.deprecate();

        when(customFieldDefinitionRepository.findByIdAndProjectId(1L, 10L)).thenReturn(Optional.of(deprecatedField));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(customFieldValueRepository.findByField_IdAndTargetTypeAndTargetId(1L, TargetType.ISSUE, 100L))
                .thenReturn(Optional.empty());

        BulkSaveCustomFieldValuesRequest request =
                new BulkSaveCustomFieldValuesRequest(List.of(new CustomFieldValueItem(1L, "x")));

        assertThatThrownBy(() -> customFieldValueService.saveValues(10L, TargetType.ISSUE, 100L, request, principal))
                .isInstanceOf(ValidationException.class);
    }

    /**
     * qa-tester 반려 사유 수정(2026-10-05): 이미 값이 존재하는 DEPRECATED 필드는 오타 수정 등
     * 정당한 "기존 값 수정"을 허용해야 한다(상세 화면이 읽기 전용이 아니라 수정 가능해야 함을
     * 뒷받침하는 서비스 레이어 동작).
     */
    @Test
    void saveValues_whenFieldIsDeprecatedButValueAlreadyExists_allowsUpdatingExistingValue() {
        CustomFieldDefinition deprecatedField = CustomFieldDefinition.builder()
                .project(project).targetType(TargetType.ISSUE).fieldKey("legacy_field")
                .label("레거시 필드").dataType(CustomFieldDataType.TEXT).required(false).displayOrder(0)
                .build();
        setId(deprecatedField, 1L);
        deprecatedField.deprecate();

        CustomFieldValue existingValue = CustomFieldValue.builder()
                .field(deprecatedField).targetType(TargetType.ISSUE).targetId(100L).value("High").build();

        when(customFieldDefinitionRepository.findByIdAndProjectId(1L, 10L)).thenReturn(Optional.of(deprecatedField));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(customFieldValueRepository.findByField_IdAndTargetTypeAndTargetId(1L, TargetType.ISSUE, 100L))
                .thenReturn(Optional.of(existingValue));
        when(customFieldDefinitionRepository.findByProjectIdAndTargetTypeAndStatusOrderByDisplayOrderAsc(
                10L, TargetType.ISSUE, CustomFieldStatus.ACTIVE)).thenReturn(List.of());
        when(customFieldDefinitionRepository.findByProjectIdAndTargetTypeAndStatusOrderByDisplayOrderAsc(
                10L, TargetType.ISSUE, CustomFieldStatus.DEPRECATED)).thenReturn(List.of(deprecatedField));
        when(customFieldValueRepository.findByField_Project_IdAndTargetTypeAndTargetId(10L, TargetType.ISSUE, 100L))
                .thenReturn(List.of(existingValue));

        BulkSaveCustomFieldValuesRequest request =
                new BulkSaveCustomFieldValuesRequest(List.of(new CustomFieldValueItem(1L, "High-FixedTypo")));

        List<CustomFieldValueResponse> result =
                customFieldValueService.saveValues(10L, TargetType.ISSUE, 100L, request, principal);

        assertThat(existingValue.getValue()).isEqualTo("High-FixedTypo");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).value()).isEqualTo("High-FixedTypo");
    }
}
