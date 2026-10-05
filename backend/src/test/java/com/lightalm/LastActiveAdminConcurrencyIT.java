package com.lightalm;

import static org.assertj.core.api.Assertions.assertThat;

import com.lightalm.domain.SystemRole;
import com.lightalm.domain.User;
import com.lightalm.dto.UpdateUserRequest;
import com.lightalm.repository.UserRepository;
import com.lightalm.service.UserService;
import com.lightalm.user.domain.LastActiveAdminRemovalException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * ADR-015 §3.4 / DoD 14 — 활성 ADMIN 2명이 서로를 동시에 강등하면 정확히 1건만 성공해야 한다
 * (SystemAdminRetentionPolicy의 PESSIMISTIC_WRITE 행 락 + PostgreSQL READ COMMITTED 재평가).
 * Testcontainers가 필요하므로 *IT.java로 명명되어 mvn verify(failsafe)에서만 실행된다(ADR-007).
 */
@Testcontainers
@SpringBootTest
class LastActiveAdminConcurrencyIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;

    @Test
    void concurrentMutualDemotion_onlyOneSucceeds() throws Exception {
        // V2 시드 admin(활성 ADMIN 1명)에 두 번째 활성 ADMIN을 더해 활성 ADMIN = 2명으로 만든다.
        User first = userRepository.findByUsername("admin").orElseThrow();
        User second = userRepository.save(User.builder()
                .username("second-admin").password("hash").email("second-admin@example.com")
                .fullName("Second Admin").systemRole(SystemRole.ADMIN).enabled(true).build());

        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Callable<Boolean>> tasks = List.of(demote(first, barrier), demote(second, barrier));
        List<Future<Boolean>> futures = executor.invokeAll(tasks);
        executor.shutdown();

        int succeeded = 0;
        int rejected = 0;
        for (Future<Boolean> future : futures) {
            try {
                future.get();
                succeeded++;
            } catch (ExecutionException ex) {
                assertThat(ex.getCause()).isInstanceOf(LastActiveAdminRemovalException.class);
                rejected++;
            }
        }

        assertThat(succeeded).isEqualTo(1);
        assertThat(rejected).isEqualTo(1);
        long activeAdmins = userRepository.findAll().stream()
                .filter(u -> u.getSystemRole() == SystemRole.ADMIN && Boolean.TRUE.equals(u.getEnabled()))
                .count();
        assertThat(activeAdmins).isEqualTo(1);
    }

    private Callable<Boolean> demote(User target, CyclicBarrier barrier) {
        return () -> {
            UpdateUserRequest request = new UpdateUserRequest();
            request.setEmail(target.getEmail());
            request.setFullName(target.getFullName());
            request.setSystemRole(SystemRole.USER);
            barrier.await();
            userService.update(target.getId(), request);
            return true;
        };
    }
}
