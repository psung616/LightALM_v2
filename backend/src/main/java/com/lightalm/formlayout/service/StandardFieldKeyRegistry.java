package com.lightalm.formlayout.service;

import com.lightalm.domain.TargetType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * ADR-012 §B.2 각주: {@code standard_field_key}의 유효값 집합은 target_type마다 다르고
 * 여러 Java 클래스(Requirement/Issue/TestCase와 그 Create/Update DTO)에 흩어져 있어
 * DB CHECK로 표현할 수 없다. 이 레지스트리가 유일한 검증 지점이다.
 *
 * <p>실제로 존재하는 생성/수정 가능한 표준 필드만 화이트리스트에 담았다(각 엔티티의
 * Create/UpdateRequest DTO 기준). 상태(status)는 요구사항/이슈에서는 별도의 상태 변경
 * 전용 API(approval 게이트가 걸려 있는 전이 포함)로만 바뀌고 생성/수정 폼의 일반 필드가
 * 아니므로 포함하지 않았다 — 단, 테스트케이스는 UpdateTestCaseRequest에 status가 실제로
 * 존재하므로 포함했다. 순서는 기존 생성/수정 화면에 보이는 기본 순서를 그대로 반영했다
 * (레이아웃 미설정 시 "기본 레이아웃"의 순서로 쓰인다).</p>
 */
@Component
public class StandardFieldKeyRegistry {

    private static final Map<TargetType, List<String>> ALLOWED_KEYS = new LinkedHashMap<>();
    private static final Map<TargetType, Map<String, String>> LABELS = new LinkedHashMap<>();

    static {
        ALLOWED_KEYS.put(TargetType.REQUIREMENT, List.of(
                "title", "description", "type", "priority", "requirementLevel",
                "parentRequirementId", "assignedTo", "dueDate"));
        ALLOWED_KEYS.put(TargetType.ISSUE, List.of(
                "title", "description", "type", "priority", "assigneeId", "dueDate"));
        ALLOWED_KEYS.put(TargetType.TEST_CASE, List.of(
                "title", "description", "preconditions", "steps", "expectedResult", "priority",
                "requirementId", "status"));

        Map<String, String> requirementLabels = new LinkedHashMap<>();
        requirementLabels.put("title", "제목");
        requirementLabels.put("description", "설명");
        requirementLabels.put("type", "유형");
        requirementLabels.put("priority", "우선순위");
        requirementLabels.put("requirementLevel", "문서 레벨(PRD/SRS)");
        requirementLabels.put("parentRequirementId", "상위 요구사항");
        requirementLabels.put("assignedTo", "담당자");
        requirementLabels.put("dueDate", "마감일");
        LABELS.put(TargetType.REQUIREMENT, requirementLabels);

        Map<String, String> issueLabels = new LinkedHashMap<>();
        issueLabels.put("title", "제목");
        issueLabels.put("description", "설명");
        issueLabels.put("type", "유형");
        issueLabels.put("priority", "우선순위");
        issueLabels.put("assigneeId", "담당자");
        issueLabels.put("dueDate", "마감일");
        LABELS.put(TargetType.ISSUE, issueLabels);

        Map<String, String> testCaseLabels = new LinkedHashMap<>();
        testCaseLabels.put("title", "제목");
        testCaseLabels.put("description", "설명");
        testCaseLabels.put("preconditions", "사전조건");
        testCaseLabels.put("steps", "실행 절차");
        testCaseLabels.put("expectedResult", "예상 결과");
        testCaseLabels.put("priority", "우선순위");
        testCaseLabels.put("requirementId", "연관 요구사항");
        testCaseLabels.put("status", "상태");
        LABELS.put(TargetType.TEST_CASE, testCaseLabels);
    }

    /** target_type의 표준 필드 키를 기본(코드 정의) 순서 그대로 반환한다. */
    public List<String> defaultOrder(TargetType targetType) {
        return ALLOWED_KEYS.getOrDefault(targetType, List.of());
    }

    public boolean isValid(TargetType targetType, String standardFieldKey) {
        return ALLOWED_KEYS.getOrDefault(targetType, List.of()).contains(standardFieldKey);
    }

    public String labelOf(TargetType targetType, String standardFieldKey) {
        return LABELS.getOrDefault(targetType, Map.of()).get(standardFieldKey);
    }
}
