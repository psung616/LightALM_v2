package com.lightalm.repository;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 요구사항 상위/하위 추적성 트리 조회 전용 리포지토리(§4.5, §4.7, §5.4).
 * PostgreSQL 재귀 CTE(WITH RECURSIVE)로 조상 체인과 자손 트리를 조회한다.
 */
@Repository
@RequiredArgsConstructor
public class TraceabilityTreeRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final String ANCESTORS_SQL = """
            WITH RECURSIVE ancestors AS (
                SELECT id, req_key, title, parent_requirement_id, 0 AS depth
                FROM requirements
                WHERE id = :reqId
                UNION ALL
                SELECT r.id, r.req_key, r.title, r.parent_requirement_id, a.depth + 1
                FROM requirements r
                JOIN ancestors a ON r.id = a.parent_requirement_id
            )
            SELECT id, req_key, title, depth FROM ancestors WHERE depth > 0 ORDER BY depth DESC
            """;

    private static final String DESCENDANTS_SQL = """
            WITH RECURSIVE descendants AS (
                SELECT id, req_key, title, status, parent_requirement_id, 1 AS depth
                FROM requirements
                WHERE parent_requirement_id = :reqId
                UNION ALL
                SELECT r.id, r.req_key, r.title, r.status, r.parent_requirement_id, d.depth + 1
                FROM requirements r
                JOIN descendants d ON r.parent_requirement_id = d.id
            )
            SELECT id, req_key, title, status, parent_requirement_id, depth FROM descendants ORDER BY depth, id
            """;

    private static final String LINKED_ISSUES_SQL = """
            SELECT tl.source_id AS requirement_id, i.id AS issue_id, i.issue_key, i.title AS issue_title,
                   tl.link_type, i.status AS issue_status
            FROM traceability_links tl
            JOIN issues i ON i.id = tl.target_id
            WHERE tl.source_type = 'REQUIREMENT' AND tl.target_type = 'ISSUE' AND tl.source_id IN (:ids)
            """;

    public List<Map<String, Object>> findAncestors(Long reqId) {
        return jdbcTemplate.queryForList(ANCESTORS_SQL, new MapSqlParameterSource("reqId", reqId));
    }

    public List<Map<String, Object>> findDescendants(Long reqId) {
        return jdbcTemplate.queryForList(DESCENDANTS_SQL, new MapSqlParameterSource("reqId", reqId));
    }

    public List<Map<String, Object>> findLinkedIssues(List<Long> requirementIds) {
        if (requirementIds.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.queryForList(LINKED_ISSUES_SQL, new MapSqlParameterSource("ids", requirementIds));
    }
}
