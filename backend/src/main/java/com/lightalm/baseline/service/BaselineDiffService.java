package com.lightalm.baseline.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.lightalm.baseline.domain.Baseline;
import com.lightalm.baseline.domain.BaselineItem;
import com.lightalm.baseline.dto.BaselineDiffResponse;
import com.lightalm.baseline.dto.BaselineFieldChange;
import com.lightalm.baseline.dto.BaselineFieldChangeKind;
import com.lightalm.baseline.dto.BaselineItemChangeType;
import com.lightalm.baseline.dto.BaselineItemDiff;
import com.lightalm.baseline.repository.BaselineItemRepository;
import com.lightalm.baseline.repository.BaselineRepository;
import com.lightalm.domain.ProjectRole;
import com.lightalm.exception.ResourceNotFoundException;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.ProjectMemberService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ADR-008(Phase 16) / 04-api.md §4.18 {@code GET .../baselines/{baselineId}/diff}.
 *
 * <p>diff는 저장하지 않는다(03-data-model.md §3.21 각주). 조회 시점에 {@code baseline_items.snapshot}과
 * {@link BaselineSnapshotFactory#captureCurrent}로 뽑은 현재 값을 {@link BaselineSnapshotJsonCodec}으로 같은
 * 형태({@link JsonNode})로 맞춘 뒤 필드 단위로 비교한다. 대상 엔티티의 값은 읽기만 한다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BaselineDiffService {

    private final BaselineRepository baselineRepository;
    private final BaselineItemRepository baselineItemRepository;
    private final BaselineSnapshotFactory snapshotFactory;
    private final BaselineSnapshotJsonCodec snapshotJsonCodec;
    private final ProjectMemberService projectMemberService;

    public BaselineDiffResponse diffAgainstCurrent(Long projectId, Long baselineId, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.VIEWER);
        Baseline baseline = baselineRepository.findByIdAndProjectId(baselineId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("베이스라인을 찾을 수 없습니다: " + baselineId));

        List<BaselineItemDiff> diffs = new ArrayList<>();
        int unchanged = 0;
        int modified = 0;
        int deleted = 0;
        for (BaselineItem item : baselineItemRepository.findByBaselineIdOrderById(baselineId)) {
            BaselineItemDiff diff = diffItem(projectId, item);
            switch (diff.changeType()) {
                case UNCHANGED -> unchanged++;
                case MODIFIED -> modified++;
                case DELETED -> deleted++;
            }
            diffs.add(diff);
        }

        return new BaselineDiffResponse(baseline.getId(), baseline.getName(), baseline.getCreatedAt(),
                LocalDateTime.now(), unchanged, modified, deleted, diffs);
    }

    private BaselineItemDiff diffItem(Long projectId, BaselineItem item) {
        JsonNode before = snapshotJsonCodec.read(item.getSnapshot());
        Optional<Map<String, Object>> current = snapshotFactory.captureCurrent(projectId, item.getTargetType(), item.getTargetId());

        if (current.isEmpty()) {
            return new BaselineItemDiff(item.getTargetType(), item.getTargetId(),
                    textOrNull(before, "key"), textOrNull(before, "title"),
                    BaselineItemChangeType.DELETED, compareFields(before, null));
        }
        JsonNode after = snapshotJsonCodec.normalize(current.get());
        List<BaselineFieldChange> changes = compareFields(before, after);
        BaselineItemChangeType type = changes.isEmpty() ? BaselineItemChangeType.UNCHANGED : BaselineItemChangeType.MODIFIED;
        return new BaselineItemDiff(item.getTargetType(), item.getTargetId(),
                textOrNull(after, "key"), textOrNull(after, "title"), type, changes);
    }

    /**
     * 필드 단위 비교. 현재 값의 필드 순서를 우선하고, 스냅샷에만 있는 필드를 뒤에 붙인다
     * (jsonb는 저장 시 키 순서를 보존하지 않으므로 현재 값(LinkedHashMap) 순서가 화면 표시에 더 자연스럽다).
     * {@code after}가 null이면 대상이 삭제된 경우로, 값이 있던 모든 필드를 REMOVED로 돌려준다.
     * JSON null과 필드 부재는 같은 "값 없음"으로 취급한다.
     */
    static List<BaselineFieldChange> compareFields(JsonNode before, JsonNode after) {
        Set<String> fields = new LinkedHashSet<>();
        if (after != null) {
            after.fieldNames().forEachRemaining(fields::add);
        }
        for (Iterator<String> it = before.fieldNames(); it.hasNext(); ) {
            fields.add(it.next());
        }

        List<BaselineFieldChange> changes = new ArrayList<>();
        for (String field : fields) {
            JsonNode oldValue = valueOrNull(before, field);
            JsonNode newValue = after != null ? valueOrNull(after, field) : null;
            if (oldValue == null && newValue == null) {
                continue;
            }
            if (oldValue != null && oldValue.equals(newValue)) {
                continue;
            }
            BaselineFieldChangeKind kind;
            if (oldValue == null) {
                kind = BaselineFieldChangeKind.ADDED;
            } else if (newValue == null) {
                kind = BaselineFieldChangeKind.REMOVED;
            } else {
                kind = BaselineFieldChangeKind.MODIFIED;
            }
            changes.add(new BaselineFieldChange(field, kind, oldValue, newValue));
        }
        return changes;
    }

    private static JsonNode valueOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value;
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = valueOrNull(node, field);
        return value != null ? value.asText() : null;
    }
}
