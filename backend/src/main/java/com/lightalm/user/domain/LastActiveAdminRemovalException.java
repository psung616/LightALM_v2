package com.lightalm.user.domain;

/**
 * ADR-015 D3. 유일한 활성 System Admin(enabled=true, system_role=ADMIN)을 ADMIN에서 해제하거나
 * 비활성화하려 할 때 던진다. {@code GlobalExceptionHandler}가 400 {@code LAST_ACTIVE_ADMIN}으로 변환한다.
 */
public class LastActiveAdminRemovalException extends RuntimeException {

    public static final String DEFAULT_MESSAGE =
            "활성 상태인 System Admin이 최소 1명은 있어야 합니다. 다른 사용자에게 System Admin 권한을 먼저 부여하세요.";

    public LastActiveAdminRemovalException() {
        super(DEFAULT_MESSAGE);
    }
}
