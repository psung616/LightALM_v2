package com.lightalm.baseline.service;

import com.lightalm.baseline.domain.Baseline;
import com.lightalm.baseline.dto.BaselineDetailResponse;
import com.lightalm.baseline.dto.BaselineItemResponse;
import com.lightalm.baseline.dto.BaselineSummaryResponse;
import com.lightalm.baseline.repository.BaselineItemRepository;
import com.lightalm.baseline.repository.BaselineRepository;
import com.lightalm.domain.ProjectRole;
import com.lightalm.exception.ResourceNotFoundException;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.ProjectMemberService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** ADR-008(Phase 16) / 04-api.md §4.18 베이스라인 목록·상세(저장된 스냅샷 그대로). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BaselineQueryService {

    private final BaselineRepository baselineRepository;
    private final BaselineItemRepository baselineItemRepository;
    private final BaselineSnapshotJsonCodec snapshotJsonCodec;
    private final ProjectMemberService projectMemberService;

    public List<BaselineSummaryResponse> list(Long projectId, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.VIEWER);
        return baselineRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(baseline -> BaselineSummaryResponse.from(baseline, baselineItemRepository.countByBaselineId(baseline.getId())))
                .toList();
    }

    public BaselineDetailResponse get(Long projectId, Long baselineId, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.VIEWER);
        Baseline baseline = baselineRepository.findByIdAndProjectId(baselineId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("베이스라인을 찾을 수 없습니다: " + baselineId));
        List<BaselineItemResponse> items = baselineItemRepository.findByBaselineIdOrderById(baselineId).stream()
                .map(item -> new BaselineItemResponse(item.getId(), item.getTargetType(), item.getTargetId(),
                        snapshotJsonCodec.read(item.getSnapshot()), item.getCapturedAt()))
                .toList();
        return BaselineDetailResponse.of(baseline, items);
    }
}
