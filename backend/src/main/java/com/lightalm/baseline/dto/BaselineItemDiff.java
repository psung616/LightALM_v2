package com.lightalm.baseline.dto;

import com.lightalm.domain.TargetType;
import java.util.List;

/**
 * 베이스라인 항목 1건의 diff. {@code key}/{@code title}은 화면 표시용으로, 대상이 남아 있으면 현재 값,
 * 삭제됐으면 스냅샷 값을 쓴다. {@code changes}는 값이 달라진 필드만 담는다(UNCHANGED면 빈 목록).
 */
public record BaselineItemDiff(
        TargetType targetType,
        Long targetId,
        String key,
        String title,
        BaselineItemChangeType changeType,
        List<BaselineFieldChange> changes) {
}
