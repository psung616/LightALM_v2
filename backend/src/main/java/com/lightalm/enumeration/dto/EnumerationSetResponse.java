package com.lightalm.enumeration.dto;

import com.lightalm.enumeration.domain.BaseEnumType;
import com.lightalm.enumeration.domain.ProjectEnumerationSet;
import com.lightalm.enumeration.domain.ProjectEnumerationValue;
import java.time.LocalDateTime;
import java.util.List;

/** 설정 화면(GET/POST /config/enumerations, PROJECT_ADMIN+)용 응답 — 값 목록(ACTIVE+DEPRECATED)을 포함한다. */
public record EnumerationSetResponse(
        Long id,
        String enumKey,
        BaseEnumType baseEnum,
        String name,
        LocalDateTime createdAt,
        List<EnumerationValueResponse> values
) {

    public static EnumerationSetResponse from(ProjectEnumerationSet set, List<ProjectEnumerationValue> values) {
        return new EnumerationSetResponse(
                set.getId(),
                set.getEnumKey(),
                set.getBaseEnum(),
                set.getName(),
                set.getCreatedAt(),
                values.stream().map(EnumerationValueResponse::from).toList());
    }
}
