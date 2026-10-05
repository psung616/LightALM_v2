package com.lightalm.customfield.api;

import com.lightalm.customfield.dto.CreateCustomFieldDefinitionRequest;
import com.lightalm.customfield.dto.CustomFieldDefinitionDetailResponse;
import com.lightalm.customfield.dto.CustomFieldDefinitionSummaryResponse;
import com.lightalm.customfield.dto.UpdateCustomFieldDefinitionRequest;
import com.lightalm.customfield.service.CustomFieldDefinitionService;
import com.lightalm.domain.TargetType;
import com.lightalm.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * ADR-012 §A.4. 커스텀 필드 정의 CRUD(소프트삭제) — 설정화면(PROJECT_ADMIN+)과
 * 폼 렌더링용 활성 목록(VIEWER+) 두 축을 함께 다룬다.
 */
@RestController
@RequestMapping("/api/projects/{projectId}")
@RequiredArgsConstructor
public class CustomFieldDefinitionController {

    private final CustomFieldDefinitionService customFieldDefinitionService;

    @GetMapping("/config/custom-fields")
    public List<CustomFieldDefinitionDetailResponse> listForConfig(@PathVariable Long projectId,
                                                                     @RequestParam TargetType targetType,
                                                                     @AuthenticationPrincipal UserPrincipal principal) {
        return customFieldDefinitionService.listForConfig(projectId, targetType, principal);
    }

    @PostMapping("/config/custom-fields")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomFieldDefinitionDetailResponse create(@PathVariable Long projectId,
                                                        @Valid @RequestBody CreateCustomFieldDefinitionRequest request,
                                                        @AuthenticationPrincipal UserPrincipal principal) {
        return customFieldDefinitionService.create(projectId, request, principal);
    }

    @PutMapping("/config/custom-fields/{fieldId}")
    public CustomFieldDefinitionDetailResponse update(@PathVariable Long projectId, @PathVariable Long fieldId,
                                                        @Valid @RequestBody UpdateCustomFieldDefinitionRequest request,
                                                        @AuthenticationPrincipal UserPrincipal principal) {
        return customFieldDefinitionService.update(projectId, fieldId, request, principal);
    }

    @DeleteMapping("/config/custom-fields/{fieldId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deprecate(@PathVariable Long projectId, @PathVariable Long fieldId,
                           @AuthenticationPrincipal UserPrincipal principal) {
        customFieldDefinitionService.deprecate(projectId, fieldId, principal);
    }

    @GetMapping("/custom-fields")
    public List<CustomFieldDefinitionSummaryResponse> listActive(@PathVariable Long projectId,
                                                                   @RequestParam TargetType targetType,
                                                                   @AuthenticationPrincipal UserPrincipal principal) {
        return customFieldDefinitionService.listActive(projectId, targetType, principal);
    }
}
