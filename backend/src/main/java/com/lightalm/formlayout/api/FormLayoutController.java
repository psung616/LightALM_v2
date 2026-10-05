package com.lightalm.formlayout.api;

import com.lightalm.domain.TargetType;
import com.lightalm.formlayout.dto.FormLayoutResponse;
import com.lightalm.formlayout.dto.SaveFormLayoutRequest;
import com.lightalm.formlayout.service.FormLayoutService;
import com.lightalm.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ADR-012 §B.3. 폼 레이아웃 조회(설정화면 PROJECT_ADMIN+ / 렌더링용 VIEWER+)와 전체 치환 저장.
 */
@RestController
@RequestMapping("/api/projects/{projectId}")
@RequiredArgsConstructor
public class FormLayoutController {

    private final FormLayoutService formLayoutService;

    @GetMapping("/config/form-layouts/{targetType}")
    public FormLayoutResponse getForConfig(@PathVariable Long projectId, @PathVariable TargetType targetType,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        return formLayoutService.getForConfig(projectId, targetType, principal);
    }

    @PutMapping("/config/form-layouts/{targetType}")
    public FormLayoutResponse save(@PathVariable Long projectId, @PathVariable TargetType targetType,
                                    @Valid @RequestBody SaveFormLayoutRequest request,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        return formLayoutService.saveLayout(projectId, targetType, request, principal);
    }

    @GetMapping("/form-layouts/{targetType}")
    public FormLayoutResponse getForRender(@PathVariable Long projectId, @PathVariable TargetType targetType,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        return formLayoutService.getForRender(projectId, targetType, principal);
    }
}
