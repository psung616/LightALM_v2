package com.lightalm.user.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.lightalm.domain.SystemRole;
import com.lightalm.domain.User;
import com.lightalm.repository.UserRepository;
import com.lightalm.user.domain.LastActiveAdminRemovalException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * ADR-015 §3.2 판정 분기. 실제 행 락(SELECT ... FOR UPDATE) 동작은 단위 테스트로 검증할 수 없어
 * 로컬 docker-compose Postgres에서 수동 확인한다(ADR-015 DoD 14).
 */
@ExtendWith(MockitoExtension.class)
class SystemAdminRetentionPolicyTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SystemAdminRetentionPolicy policy;

    private static User admin(long id) {
        return User.builder().id(id).username("admin" + id).password("hash").email("admin" + id + "@example.com")
                .fullName("Admin " + id).systemRole(SystemRole.ADMIN).enabled(true).build();
    }

    @Test
    void requireAnotherActiveAdmin_whenTargetIsTheOnlyActiveAdmin_throws() {
        when(userRepository.findEnabledAdminsForUpdate()).thenReturn(List.of(admin(1L)));

        assertThatThrownBy(() -> policy.requireAnotherActiveAdmin(1L))
                .isInstanceOf(LastActiveAdminRemovalException.class)
                .hasMessageContaining("System Admin");
    }

    @Test
    void requireAnotherActiveAdmin_whenAnotherActiveAdminRemains_passes() {
        when(userRepository.findEnabledAdminsForUpdate()).thenReturn(List.of(admin(1L), admin(2L)));

        assertThatCode(() -> policy.requireAnotherActiveAdmin(1L)).doesNotThrowAnyException();
    }

    @Test
    void requireAnotherActiveAdmin_whenTargetIsNotActiveAdmin_passesEvenIfOnlyOneAdminRemains() {
        // 대상(2)은 USER이거나 비활성 ADMIN이라 락 조회 결과에 없다 — 활성 ADMIN 수와 무관하게 통과.
        when(userRepository.findEnabledAdminsForUpdate()).thenReturn(List.of(admin(1L)));

        assertThatCode(() -> policy.requireAnotherActiveAdmin(2L)).doesNotThrowAnyException();
    }

    @Test
    void requireAnotherActiveAdmin_whenConcurrentDemotionAlreadyCommitted_throwsForRemainingAdmin() {
        // 동시 강등 시나리오(§3.4): 상대 트랜잭션이 먼저 커밋해 락 재평가 결과가 {2}만 남은 경우.
        when(userRepository.findEnabledAdminsForUpdate()).thenReturn(List.of(admin(2L)));

        assertThatThrownBy(() -> policy.requireAnotherActiveAdmin(2L))
                .isInstanceOf(LastActiveAdminRemovalException.class);
    }

    @Test
    void requireAnotherActiveAdmin_whenNoActiveAdminAtAll_passesForNonAdminTarget() {
        when(userRepository.findEnabledAdminsForUpdate()).thenReturn(List.of());

        assertThatCode(() -> policy.requireAnotherActiveAdmin(5L)).doesNotThrowAnyException();
    }
}
