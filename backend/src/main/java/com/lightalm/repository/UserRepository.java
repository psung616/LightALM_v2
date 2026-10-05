package com.lightalm.repository;

import com.lightalm.domain.User;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /**
     * ADR-015 §3.4. 활성 System Admin 전체를 행 락(SELECT ... FOR UPDATE)으로 조회한다.
     * id 오름차순으로 락 획득 순서를 고정해 동시 강등 요청 간 데드락을 막는다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.systemRole = com.lightalm.domain.SystemRole.ADMIN and u.enabled = true order by u.id")
    List<User> findEnabledAdminsForUpdate();
}
