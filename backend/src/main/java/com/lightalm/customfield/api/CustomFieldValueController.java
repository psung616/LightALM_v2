package com.lightalm.customfield.api;

import com.lightalm.customfield.dto.BulkSaveCustomFieldValuesRequest;
import com.lightalm.customfield.dto.CustomFieldValueResponse;
import com.lightalm.customfield.service.CustomFieldValueService;
import com.lightalm.domain.TargetType;
import com.lightalm.exception.ValidationException;
import com.lightalm.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ADR-012 §A.4. 대상(요구사항/이슈/테스트케이스)의 커스텀 필드 값 조회/일괄저장.
 */
@RestController
@RequestMapping("/api/projects/{projectId}")
@RequiredArgsConstructor
public class CustomFieldValueController {

    private final CustomFieldValueService customFieldValueService;

    @GetMapping("/{targetType}/{targetId}/custom-field-values")
    public List<CustomFieldValueResponse> list(@PathVariable Long projectId, @PathVariable String targetType,
                                                 @PathVariable Long targetId,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return customFieldValueService.listValues(projectId, toTargetType(targetType), targetId, principal);
    }

    @PutMapping("/{targetType}/{targetId}/custom-field-values")
    public List<CustomFieldValueResponse> save(@PathVariable Long projectId, @PathVariable String targetType,
                                                 @PathVariable Long targetId,
                                                 @Valid @RequestBody BulkSaveCustomFieldValuesRequest request,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return customFieldValueService.saveValues(projectId, toTargetType(targetType), targetId, request, principal);
    }

    private TargetType toTargetType(String pathSegment) {
        return switch (pathSegment) {
            case "requirements" -> TargetType.REQUIREMENT;
            case "issues" -> TargetType.ISSUE;
            case "test-cases" -> TargetType.TEST_CASE;
            default -> throw new ValidationException("targetType은 requirements, issues, test-cases 중 하나여야 합니다.");
        };
    }
}
