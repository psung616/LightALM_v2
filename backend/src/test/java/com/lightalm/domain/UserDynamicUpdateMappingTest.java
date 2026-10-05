package com.lightalm.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.annotations.DynamicUpdate;
import org.junit.jupiter.api.Test;

/**
 * ADR-016 D3 회귀 방지. {@code @DynamicUpdate}가 빠지면 email/fullName만 수정하는 UPDATE가
 * 동시 커밋된 system_role/enabled를 옛 값으로 되돌릴 수 있다(활성 ADMIN 0명 가능).
 * 실제 생성 SQL은 로컬 Postgres + {@code org.hibernate.SQL} 로그로 확인했다(ADR-016 구현 각주).
 */
class UserDynamicUpdateMappingTest {

    @Test
    void userEntity_updatesOnlyDirtyColumns() {
        assertThat(User.class.isAnnotationPresent(DynamicUpdate.class)).isTrue();
    }
}
