package com.lightalm.baseline.service;

import com.lightalm.domain.Issue;
import com.lightalm.domain.Requirement;
import com.lightalm.domain.TargetType;
import com.lightalm.domain.TestCase;
import com.lightalm.repository.IssueRepository;
import com.lightalm.repository.RequirementRepository;
import com.lightalm.repository.TestCaseRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * ADR-008(Phase 16) / 03-data-model.md §3.21. 요구사항/이슈/테스트케이스의 "현재" 주요 필드 값을
 * 베이스라인 스냅샷과 같은 모양(Map&lt;필드명, 값&gt;)으로 뽑아낸다. 베이스라인 생성 시점의
 * 스냅샷 캡처와, diff 계산 시점의 "현재 값" 조회 양쪽에서 재사용한다(동일한 필드 집합이어야
 * 비교가 의미 있기 때문).
 *
 * <p>{@code key}(요구사항/이슈/테스트케이스 표시 키)는 원본에서 바뀌지 않는 값이지만, 대상이 삭제된 뒤에도
 * diff 화면에서 어떤 항목이었는지 식별할 수 있도록 스냅샷에 함께 남긴다.</p>
 */
@Component
@RequiredArgsConstructor
public class BaselineSnapshotFactory {

    private final RequirementRepository requirementRepository;
    private final IssueRepository issueRepository;
    private final TestCaseRepository testCaseRepository;

    /** 대상이 이 프로젝트에 존재하면 현재 필드 값 맵을 반환하고, 삭제되었거나 없으면 empty. */
    public Optional<Map<String, Object>> captureCurrent(Long projectId, TargetType targetType, Long targetId) {
        return switch (targetType) {
            case REQUIREMENT -> requirementRepository.findById(targetId)
                    .filter(r -> r.getProject().getId().equals(projectId))
                    .map(this::fromRequirement);
            case ISSUE -> issueRepository.findById(targetId)
                    .filter(i -> i.getProject().getId().equals(projectId))
                    .map(this::fromIssue);
            case TEST_CASE -> testCaseRepository.findById(targetId)
                    .filter(t -> t.getProject().getId().equals(projectId))
                    .map(this::fromTestCase);
        };
    }

    private Map<String, Object> fromRequirement(Requirement r) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("key", r.getReqKey());
        snapshot.put("title", r.getTitle());
        snapshot.put("description", r.getDescription());
        snapshot.put("type", r.getType() != null ? r.getType().name() : null);
        snapshot.put("priority", r.getPriority());
        snapshot.put("status", r.getStatus() != null ? r.getStatus().name() : null);
        snapshot.put("requirementLevel", r.getRequirementLevel() != null ? r.getRequirementLevel().name() : null);
        snapshot.put("assignedToId", r.getAssignedTo() != null ? r.getAssignedTo().getId() : null);
        snapshot.put("dueDate", r.getDueDate() != null ? r.getDueDate().toString() : null);
        return snapshot;
    }

    private Map<String, Object> fromIssue(Issue i) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("key", i.getIssueKey());
        snapshot.put("title", i.getTitle());
        snapshot.put("description", i.getDescription());
        snapshot.put("type", i.getType() != null ? i.getType().name() : null);
        snapshot.put("priority", i.getPriority());
        snapshot.put("status", i.getStatus() != null ? i.getStatus().name() : null);
        snapshot.put("assigneeId", i.getAssignee() != null ? i.getAssignee().getId() : null);
        snapshot.put("dueDate", i.getDueDate() != null ? i.getDueDate().toString() : null);
        return snapshot;
    }

    private Map<String, Object> fromTestCase(TestCase t) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("key", t.getTcKey());
        snapshot.put("title", t.getTitle());
        snapshot.put("description", t.getDescription());
        snapshot.put("preconditions", t.getPreconditions());
        snapshot.put("steps", t.getSteps());
        snapshot.put("expectedResult", t.getExpectedResult());
        snapshot.put("priority", t.getPriority());
        snapshot.put("status", t.getStatus() != null ? t.getStatus().name() : null);
        return snapshot;
    }
}
