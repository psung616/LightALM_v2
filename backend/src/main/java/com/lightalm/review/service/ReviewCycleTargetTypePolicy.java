package com.lightalm.review.service;

import com.lightalm.domain.TargetType;
import com.lightalm.exception.ValidationException;
import java.util.Set;

/** 리뷰 사이클 대상은 REQUIREMENT/ISSUE만 허용한다(03-data-model.md §3.18 CHECK 제약과 동일). */
final class ReviewCycleTargetTypePolicy {

    private static final Set<TargetType> SUPPORTED = Set.of(TargetType.REQUIREMENT, TargetType.ISSUE);

    private ReviewCycleTargetTypePolicy() {
    }

    static void requireSupported(TargetType targetType) {
        if (!SUPPORTED.contains(targetType)) {
            throw new ValidationException("리뷰 사이클은 REQUIREMENT/ISSUE target_type만 지원합니다: " + targetType);
        }
    }
}
