package com.lightalm.baseline.service;

import com.lightalm.baseline.domain.Baseline;
import com.lightalm.baseline.domain.BaselineItem;
import com.lightalm.baseline.dto.BaselineDetailResponse;
import com.lightalm.baseline.dto.BaselineItemRef;
import com.lightalm.baseline.dto.BaselineItemResponse;
import com.lightalm.baseline.dto.CreateBaselineRequest;
import com.lightalm.baseline.repository.BaselineItemRepository;
import com.lightalm.baseline.repository.BaselineRepository;
import com.lightalm.domain.Project;
import com.lightalm.domain.ProjectRole;
import com.lightalm.domain.User;
import com.lightalm.exception.ResourceNotFoundException;
import com.lightalm.repository.UserRepository;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.ProjectMemberService;
import com.lightalm.service.ProjectService;
import com.lightalm.service.support.PolymorphicTargetValidator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ADR-008(Phase 16) / 04-api.md §4.18 {@code POST .../baselines}. 생성 시점 각 대상의 주요 필드를
 * {@link BaselineSnapshotFactory}로 뽑아 jsonb 스냅샷으로 얼려 저장한다. 이후 스냅샷은 절대 수정하지 않는다.
 * 대상 엔티티의 값은 읽기만 한다.
 */
@Service
@RequiredArgsConstructor
public class BaselineCommandService {

    private final BaselineRepository baselineRepository;
    private final BaselineItemRepository baselineItemRepository;
    private final BaselineSnapshotFactory snapshotFactory;
    private final BaselineSnapshotJsonCodec snapshotJsonCodec;
    private final PolymorphicTargetValidator polymorphicTargetValidator;
    private final UserRepository userRepository;
    private final ProjectService projectService;
    private final ProjectMemberService projectMemberService;

    @Transactional
    public BaselineDetailResponse create(Long projectId, CreateBaselineRequest request, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.PROJECT_ADMIN);

        // 같은 (targetType, targetId)가 중복으로 들어와도 UNIQUE 제약 위반(500) 대신 1건으로 합친다.
        Set<BaselineItemRef> refs = new LinkedHashSet<>(request.itemRefs());
        Map<BaselineItemRef, String> snapshots = new LinkedHashMap<>();
        for (BaselineItemRef ref : refs) {
            polymorphicTargetValidator.ensureExists(projectId, ref.targetType(), ref.targetId());
            Map<String, Object> current = snapshotFactory.captureCurrent(projectId, ref.targetType(), ref.targetId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "베이스라인 대상을 찾을 수 없습니다: " + ref.targetType() + "#" + ref.targetId()));
            snapshots.put(ref, snapshotJsonCodec.write(current));
        }

        Project project = projectService.getEntity(projectId);
        User creator = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + principal.getId()));

        Baseline baseline = Baseline.builder()
                .project(project)
                .name(request.name())
                .description(request.description())
                .createdBy(creator)
                .build();
        baselineRepository.save(baseline);

        List<BaselineItem> items = snapshots.entrySet().stream()
                .map(entry -> BaselineItem.builder()
                        .baseline(baseline)
                        .targetType(entry.getKey().targetType())
                        .targetId(entry.getKey().targetId())
                        .snapshot(entry.getValue())
                        .build())
                .toList();
        baselineItemRepository.saveAll(items);

        List<BaselineItemResponse> itemResponses = items.stream()
                .map(item -> new BaselineItemResponse(item.getId(), item.getTargetType(), item.getTargetId(),
                        snapshotJsonCodec.read(item.getSnapshot()), item.getCapturedAt()))
                .toList();
        return BaselineDetailResponse.of(baseline, itemResponses);
    }
}
