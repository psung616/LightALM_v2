package com.lightalm.user.service;

import com.lightalm.domain.User;
import com.lightalm.repository.UserRepository;
import com.lightalm.user.domain.LastActiveAdminRemovalException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * ADR-015 D3/§3. 불변식 "활성(enabled=true) ADMIN이 항상 1명 이상"을 지키는 판정 로직.
 *
 * <p>활성 ADMIN 행 전체를 {@code PESSIMISTIC_WRITE}(SELECT ... FOR UPDATE, id 오름차순)로 잠근 뒤,
 * 대상이 그 집합에 들어 있고 집합 크기가 1 이하이면 거부한다. 대상이 활성 ADMIN인지는 영속성
 * 컨텍스트에 이미 올라온 엔티티 필드가 아니라 <b>락 조회 결과에 들어 있는지</b>로 판단한다
 * (동시 트랜잭션이 커밋한 최신 값 기준, §3.2). 락이 이어지는 변경과 같은 트랜잭션 안에서
 * 유지되어야 하므로 호출자의 트랜잭션을 필수로 요구한다({@link Propagation#MANDATORY}).</p>
 */
@Service
@RequiredArgsConstructor
public class SystemAdminRetentionPolicy {

    private final UserRepository userRepository;

    /**
     * 대상 사용자를 활성 ADMIN 집합에서 빠지게 하는 변경(ADMIN 해제, 비활성화) 직전에 호출한다.
     * 대상이 현재 활성 ADMIN이 아니면 통과, 활성 ADMIN이 대상 1명뿐이면 {@link LastActiveAdminRemovalException}.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void requireAnotherActiveAdmin(Long targetUserId) {
        List<User> lockedAdmins = userRepository.findEnabledAdminsForUpdate();
        boolean targetIsActiveAdmin = lockedAdmins.stream().anyMatch(admin -> admin.getId().equals(targetUserId));
        if (!targetIsActiveAdmin) {
            return;
        }
        if (lockedAdmins.size() <= 1) {
            throw new LastActiveAdminRemovalException();
        }
    }
}
