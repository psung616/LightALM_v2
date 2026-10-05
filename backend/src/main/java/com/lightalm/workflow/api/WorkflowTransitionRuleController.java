package com.lightalm.workflow.api;

import com.lightalm.domain.TargetType;
import com.lightalm.security.UserPrincipal;
import com.lightalm.workflow.dto.CreateWorkflowTransitionRuleRequest;
import com.lightalm.workflow.dto.NextStatusesResponse;
import com.lightalm.workflow.dto.WorkflowRuleListResponse;
import com.lightalm.workflow.dto.WorkflowTransitionRuleResponse;
import com.lightalm.workflow.service.WorkflowTransitionRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * ADR-012 §D.4. 워크플로우 전이 규칙 CRUD(설정화면, PROJECT_ADMIN+)와 다음 상태 조회(VIEWER+).
 */
@RestController
@RequestMapping("/api/projects/{projectId}")
@RequiredArgsConstructor
public class WorkflowTransitionRuleController {

    private final WorkflowTransitionRuleService workflowTransitionRuleService;

    @GetMapping("/config/workflow-rules")
    public WorkflowRuleListResponse list(@PathVariable Long projectId,
                                          @RequestParam TargetType targetType,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        return workflowTransitionRuleService.list(projectId, targetType, principal);
    }

    @PostMapping("/config/workflow-rules")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowTransitionRuleResponse create(@PathVariable Long projectId,
                                                   @Valid @RequestBody CreateWorkflowTransitionRuleRequest request,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        return workflowTransitionRuleService.create(projectId, request, principal);
    }

    @DeleteMapping("/config/workflow-rules/{ruleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long projectId, @PathVariable Long ruleId,
                        @AuthenticationPrincipal UserPrincipal principal) {
        workflowTransitionRuleService.delete(projectId, ruleId, principal);
    }

    @GetMapping("/workflow-rules/{targetType}/{fromStatus}")
    public NextStatusesResponse nextStatuses(@PathVariable Long projectId,
                                              @PathVariable TargetType targetType,
                                              @PathVariable String fromStatus,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        return workflowTransitionRuleService.nextStatuses(projectId, targetType, fromStatus, principal);
    }
}
