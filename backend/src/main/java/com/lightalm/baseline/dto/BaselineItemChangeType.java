package com.lightalm.baseline.dto;

/** 베이스라인 항목 1건의 diff 결과 분류. */
public enum BaselineItemChangeType {
    /** 스냅샷과 현재 값이 모든 필드에서 같음. */
    UNCHANGED,
    /** 대상은 존재하지만 하나 이상의 필드 값이 바뀜. */
    MODIFIED,
    /** 대상이 삭제되었거나 더 이상 이 프로젝트에 속하지 않음(현재 값 없음). */
    DELETED
}
