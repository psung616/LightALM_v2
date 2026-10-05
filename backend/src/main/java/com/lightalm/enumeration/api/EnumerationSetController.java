package com.lightalm.enumeration.api;

import com.lightalm.enumeration.dto.CreateEnumerationSetRequest;
import com.lightalm.enumeration.dto.CreateEnumerationValueRequest;
import com.lightalm.enumeration.dto.EnumerationSetResponse;
import com.lightalm.enumeration.dto.EnumerationValueResponse;
import com.lightalm.enumeration.dto.UpdateEnumerationValueRequest;
import com.lightalm.enumeration.service.EnumerationSetService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * ADR-012 §C.4. 열거형 집합/값 CRUD(설정화면, PROJECT_ADMIN+)와 폼 렌더링용 활성 값 조회(VIEWER+).
 */
@RestController
@RequestMapping("/api/projects/{projectId}")
@RequiredArgsConstructor
public class EnumerationSetController {

    private final EnumerationSetService enumerationSetService;

    @GetMapping("/config/enumerations")
    public List<EnumerationSetResponse> list(@PathVariable Long projectId,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        return enumerationSetService.list(projectId, principal);
    }

    @PostMapping("/config/enumerations")
    @ResponseStatus(HttpStatus.CREATED)
    public EnumerationSetResponse create(@PathVariable Long projectId,
                                          @Valid @RequestBody CreateEnumerationSetRequest request,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        return enumerationSetService.create(projectId, request, principal);
    }

    @PostMapping("/config/enumerations/{id}/values")
    @ResponseStatus(HttpStatus.CREATED)
    public EnumerationValueResponse addValue(@PathVariable Long projectId, @PathVariable Long id,
                                              @Valid @RequestBody CreateEnumerationValueRequest request,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        return enumerationSetService.addValue(projectId, id, request, principal);
    }

    @PutMapping("/config/enumerations/{id}/values/{valueId}")
    public EnumerationValueResponse updateValue(@PathVariable Long projectId, @PathVariable Long id,
                                                 @PathVariable Long valueId,
                                                 @Valid @RequestBody UpdateEnumerationValueRequest request,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return enumerationSetService.updateValue(projectId, id, valueId, request, principal);
    }

    @DeleteMapping("/config/enumerations/{id}/values/{valueId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deprecateValue(@PathVariable Long projectId, @PathVariable Long id, @PathVariable Long valueId,
                                @AuthenticationPrincipal UserPrincipal principal) {
        enumerationSetService.deprecateValue(projectId, id, valueId, principal);
    }

    @GetMapping("/enumerations/{enumKey}/values")
    public List<EnumerationValueResponse> activeValues(@PathVariable Long projectId, @PathVariable String enumKey,
                                                         @AuthenticationPrincipal UserPrincipal principal) {
        return enumerationSetService.listActiveValues(projectId, enumKey, principal);
    }
}
